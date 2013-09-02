package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Direction;
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
    protected Neo4JUtils neo4JUtils;

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
        Node sameAsAsNode = neo4JUtils.getFromUri(
                friendlyResource.uri()
        );
        node.createRelationshipTo(
                sameAsAsNode,
                Relationships.SAME_AS
        );
        updateLastModificationDate();
    }

    @Override
    public Set<FriendlyResource> getSameAs() {
        Set<FriendlyResource> sameAsSet = new HashSet<FriendlyResource>();
        for (Relationship relationship : node.getRelationships(Relationships.SAME_AS)) {
            FriendlyResource sameAs = friendlyResourceFactory.createOrLoadFromNode(
                    relationship.getEndNode()
            );
            sameAsSet.add(sameAs);
        }
        return sameAsSet;
    }

    @Override
    public void addType(FriendlyResource type) {
        Node typeAsNode = neo4JUtils.getFromUri(
                type.uri()
        );
        node.createRelationshipTo(
                typeAsNode,
                Relationships.TYPE
        );
        updateLastModificationDate();
    }

    @Override
    public void removeFriendlyResource(FriendlyResource friendlyResource) {
        Node friendlyResourceAsNode = neo4JUtils.getFromUri(
                friendlyResource.uri()
        );
        for (Relationship relationship : node.getRelationships(Direction.OUTGOING)) {
            Node endNode = relationship.getEndNode();
            if (endNode.equals(friendlyResourceAsNode)) {
                relationship.delete();
            }
        }
        updateLastModificationDate();
    }

    @Override
    public Set<FriendlyResource> getAdditionalTypes() {
        Set<FriendlyResource> additionalTypes = new HashSet<FriendlyResource>();
        for (Relationship relationship : node.getRelationships(Relationships.TYPE)) {
            FriendlyResource type = friendlyResourceFactory.createOrLoadFromNode(
                    relationship.getEndNode()
            );
            if (!type.uri().toString().equals(TripleBrainUris.TRIPLE_BRAIN_VERTEX)) {
                additionalTypes.add(type);
            }
        }
        return additionalTypes;
    }

    @Override
    public void remove(){
        for (Relationship relationship : node.getRelationships()) {
            //removing explicitly so node index gets reindexed
            neo4JUtils.removeAllProperties(
                    relationship
            );
            relationship.delete();
        }
        //removing explicitly so node index gets reindexed
        node.removeProperty(Neo4JUserGraph.URI_PROPERTY_NAME);
        node.delete();
    }

    private String removeEnclosingCharsOfListAsString(String listAsString) {
        return listAsString.substring(
                1,
                listAsString.length() - 1
        );
    }
}
