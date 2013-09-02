package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.TripleBrainUris;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.graph.GraphElement;

import javax.inject.Inject;
import java.net.URI;
import java.util.*;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JGraphElement implements GraphElement {

    public static final String CREATION_DATE_PROPERTY_NAME = "creation_date";
    public static final String LAST_MODIFICATION_DATE_PROPERTY_NAME = "last_modification_date";
    private Node node;
    private User owner;

    @Inject
    private Neo4JFriendlyResourceFactory friendlyResourceFactory;

    @Inject
    protected Neo4JUtils utils;

    @AssistedInject
    protected Neo4JGraphElement(
            @Assisted Node node,
            @Assisted User owner
    ) {
        this.node = node;
        this.owner = owner;
    }

    @AssistedInject
    protected Neo4JGraphElement(
            @Assisted Node node,
            @Assisted URI uri,
            @Assisted User owner
    ) {
        this(
                node,
                owner
        );
        node.setProperty(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        );
        this.label("");
        Long creationDate = new Date().getTime();
        node.setProperty(
                CREATION_DATE_PROPERTY_NAME,
                creationDate
        );
        node.setProperty(
                LAST_MODIFICATION_DATE_PROPERTY_NAME,
                creationDate
        );
    }

    @Override
    public DateTime creationDate() {
        return new DateTime((Long) node.getProperty(
                CREATION_DATE_PROPERTY_NAME
        ));
    }

    @Override
    public DateTime lastModificationDate() {
        return new DateTime((Long) node.getProperty(
                LAST_MODIFICATION_DATE_PROPERTY_NAME
        ));
    }

    public void updateLastModificationDate() {
        node.setProperty(
                LAST_MODIFICATION_DATE_PROPERTY_NAME,
                new Date().getTime()
        );
    }

    @Override
    public URI uri() {
        return Uris.get(
                node.getProperty(
                        Neo4JUserGraph.URI_PROPERTY_NAME
                ).toString()
        );
    }

    @Override
    public String label() {
        return node.getProperty(RDFS.label.getURI()).toString();
    }

    @Override
    public void label(String label) {
        node.setProperty(RDFS.label.getURI(), label);
        updateLastModificationDate();
    }

    @Override
    public boolean hasLabel() {
        return node.hasProperty(RDFS.label.getURI());
    }

    @Override
    public User owner() {
        return owner;
    }

    @Override
    public void addSameAs(FriendlyResource friendlyResource) {
        addResourceUriConnectedWithRelationName(
                friendlyResource.uri().toString(),
                Relationships.SAME_AS.name()
        );
        updateLastModificationDate();
    }

    @Override
    public Set<FriendlyResource> getSameAs() {
        return getExternalFriendlyResourcesOfRelation(
                Relationships.SAME_AS.name()
        );
    }

    @Override
    public void addType(FriendlyResource type) {
        addResourceUriConnectedWithRelationName(
                type.uri().toString(),
                Relationships.TYPE.name()
        );
        updateLastModificationDate();
    }

    @Override
    public void removeFriendlyResource(FriendlyResource friendlyResource) {
        removeRelationToExternalResource(
                Relationships.SAME_AS.name(),
                friendlyResource
        );
        removeRelationToExternalResource(
                Relationships.TYPE.name(),
                friendlyResource
        );
        updateLastModificationDate();
    }

    @Override
    public Set<FriendlyResource> getAdditionalTypes() {
        Set<FriendlyResource> friendlyResourceImpls = getExternalFriendlyResourcesOfRelation(
                Relationships.TYPE.name()
        );
        friendlyResourceImpls.remove(friendlyResourceFactory.createOrLoadFromUri(
                Uris.get(TripleBrainUris.TRIPLE_BRAIN_VERTEX)
        ));
        return friendlyResourceImpls;
    }

    public void remove(){
        for (Relationship relationship : node.getRelationships()) {
            utils.removeAllProperties(
                    relationship
            );
            relationship.delete();
        }
        //removing explicitly so index
        node.removeProperty(Neo4JUserGraph.URI_PROPERTY_NAME);
        node.delete();
    }

    private String removeEnclosingCharsOfListAsString(String listAsString) {
        return listAsString.substring(
                1,
                listAsString.length() - 1
        );
    }

    private Set<FriendlyResource> getExternalFriendlyResourcesOfRelation(String relationName) {
        Set<FriendlyResource> set = new HashSet<FriendlyResource>();
        List<String> uris = getListOfResourcesUriConnectedWithRelationshipName(
                relationName
        );
        for (String uri : uris) {
            set.add(
                    friendlyResourceFactory.createOrLoadFromUri(
                            Uris.get(uri)
                    )
            );
        }
        return set;
    }

    private void addResourceUriConnectedWithRelationName(String uri, String relationName) {
        List<String> uris = getListOfResourcesUriConnectedWithRelationshipName(
                relationName
        );
        uris.add(
                uri
        );
        node.setProperty(
                relationName,
                uris.toString()
        );
    }

    private List<String> getSameAsResourcesUri() {
        return getListOfResourcesUriConnectedWithRelationshipName(
                Relationships.SAME_AS.name()
        );
    }

    private List<String> getTypeResourcesUri() {
        return getListOfResourcesUriConnectedWithRelationshipName(
                Relationships.TYPE.name()
        );
    }

    private List<String> getListOfResourcesUriConnectedWithRelationshipName(String propertyName) {
        if (!node.hasProperty(propertyName)) {
            return new ArrayList<String>();
        }
        String urisAsString = (String) node.getProperty(
                propertyName
        );
        urisAsString = removeEnclosingCharsOfListAsString(
                urisAsString
        );
        if (urisAsString.isEmpty()) {
            return new ArrayList<String>();
        }
        return new ArrayList<String>(
                Arrays.asList(
                        urisAsString.split("\\s*,\\s*")
                )
        );
    }

    private void removeRelationToExternalResource(String relationName, FriendlyResource friendlyResource) {
        List<String> uris = getListOfResourcesUriConnectedWithRelationshipName(
                relationName
        );
        boolean wasUriRemoved = uris.remove(
                friendlyResource.uri().toString()
        );
        if (wasUriRemoved) {
            node.setProperty(
                    relationName,
                    uris.toString()
            );
        }
    }
}
