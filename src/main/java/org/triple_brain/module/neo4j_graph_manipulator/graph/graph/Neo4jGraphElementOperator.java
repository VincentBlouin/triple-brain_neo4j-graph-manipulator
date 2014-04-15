package org.triple_brain.module.neo4j_graph_manipulator.graph.graph;

import com.google.api.client.util.DateTime;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.batch.BatchCallback;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.FriendlyResourcePojo;
import org.triple_brain.module.model.graph.GraphElementOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.*;
import javax.inject.Inject;
import java.net.URI;
import java.util.*;
import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jGraphElementOperator implements GraphElementOperator, Neo4jOperator {

    protected Node node;
    protected Neo4jFriendlyResource friendlyResource;
    protected Neo4jFriendlyResourceFactory friendlyResourceFactory;
    protected URI uri;
    protected QueryEngine<Map<String, Object>> queryEngine;
    protected RestAPI restApi;

    @Inject
    protected Neo4jUtils neo4jUtils;

    @AssistedInject
    protected Neo4jGraphElementOperator(
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            QueryEngine queryEngine,
            RestAPI restApi,
            @Assisted Node node
    ) {
        friendlyResource = friendlyResourceFactory.withNode(
                node
        );
        this.friendlyResourceFactory = friendlyResourceFactory;
        this.node = node;
        this.queryEngine = queryEngine;
        this.restApi = restApi;
    }

    @AssistedInject
    protected Neo4jGraphElementOperator(
            Neo4jUtils utils,
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            QueryEngine queryEngine,
            RestAPI restApi,
            @Assisted URI uri
    ) {
        friendlyResource = friendlyResourceFactory.withUri(
                uri
        );
        this.friendlyResourceFactory = friendlyResourceFactory;
        this.neo4jUtils = utils;
        this.queryEngine = queryEngine;
        this.restApi = restApi;
    }

    @Override
    public Date creationDate() {
        return friendlyResource.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return friendlyResource.lastModificationDate();
    }

    public void updateLastModificationDate() {
        friendlyResource.updateLastModificationDate();
    }

    @Override
    public URI uri() {
        return friendlyResource.uri();
    }

    @Override
    public String label() {
        return friendlyResource.label();
    }

    @Override
    public void label(String label) {
        friendlyResource.label(
                label
        );
    }

    @Override
    public Set<Image> images() {
        return friendlyResource.images();
    }

    @Override
    public Boolean gotImages() {
        return friendlyResource.gotImages();
    }

    @Override
    public String comment() {
        return friendlyResource.comment();
    }

    @Override
    public void comment(String comment) {
        friendlyResource.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return friendlyResource.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        friendlyResource.addImages(
                images
        );
    }

    @Override
    public boolean hasLabel() {
        return friendlyResource.hasLabel();
    }

    @Override
    public FriendlyResourcePojo addGenericIdentification(FriendlyResource genericIdentification) throws IllegalArgumentException {
        return addIdentificationUsingRelation(
                genericIdentification,
                Relationships.IDENTIFIED_TO
        );
    }

    @Override
    public void create() {
        createUsingInitialValues(
                map()
        );
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        friendlyResource.createUsingInitialValues(values);
    }

    @Override
    public Map<URI,FriendlyResource> getGenericIdentifications() {
        return getIdentificationUsingRelation(
                Relationships.IDENTIFIED_TO
        );
    }

    @Override
    public FriendlyResourcePojo addSameAs(FriendlyResource sameAs) throws IllegalArgumentException {
        return addIdentificationUsingRelation(
                sameAs,
                Relationships.SAME_AS
        );
    }

    private FriendlyResourcePojo addIdentificationUsingRelation(
            final FriendlyResource identification,
            final Relationships relation
    ) {
        ifIdentificationIsSelfThrowException(identification);
        final Neo4jFriendlyResource neo4jFriendlyResource = friendlyResourceFactory.withUri(
                identification.uri()
        );
        QueryResult<Map<String,Object>> results = restApi.executeBatch(new BatchCallback<QueryResult<Map<String,Object>>>() {
            @Override
            public QueryResult<Map<String,Object>> recordBatch(RestAPI restApi) {
                 QueryResult<Map<String,Object>> results = queryEngine.query(
                        queryPrefix() +
                                "MERGE (f {" +
                                "uri: {uri} " +
                                "}) " +
                                "ON CREATE SET " +
                                "f.`" + RDFS.label.getURI() + "`={label}, " +
                                "f.`" + RDFS.comment.getURI() + "`={comment}, " +
                                "f." + Neo4jFriendlyResource.props.creation_date + "=timestamp(), " +
                                "f." + Neo4jFriendlyResource.props.last_modification_date + "=timestamp() " +
                                "CREATE UNIQUE " +
                                "n-[:" + relation + "]->f " +
                                "RETURN " +
                                "f.uri as uri, " +
                                "f.`" + RDFS.label.getURI() + "` as label, " +
                                "f.`" + RDFS.comment.getURI() + "` as comment, " +
                                "f." + Neo4jFriendlyResource.props.creation_date + " as creation_date, " +
                                "f." + Neo4jFriendlyResource.props.last_modification_date + " as last_modification_date",
                        neo4jFriendlyResource.addCreationProperties(
                                map(
                                        "label",
                                        identification.label(),
                                        "comment",
                                        identification.comment()
                                )
                        )
                );
                updateLastModificationDate();
                return results;
            }
        });
        Map<String,Object> result = results.iterator().next();
        return new FriendlyResourcePojo(
                URI.create(
                        result.get("uri").toString()
                ),
                result.get("label") == null ?
                        "" : result.get("label").toString(),
                new HashSet<Image>(),
                result.get("comment") == null ? "" : result.get("comment").toString(),
                new Date(
                        (Long) result.get("creation_date")
                ),
                new Date(
                        (Long) result.get("last_modification_date")
                )
        );
    }

    @Override
    public Map<URI, FriendlyResource> getSameAs() {
        return getIdentificationUsingRelation(
                Relationships.SAME_AS
        );
    }

    @Override
    public FriendlyResourcePojo addType(FriendlyResource type) throws IllegalArgumentException {
        return addIdentificationUsingRelation(
                type,
                Relationships.TYPE
        );
    }

    @Override
    public void removeIdentification(FriendlyResource friendlyResource) {
        Node friendlyResourceAsNode = friendlyResourceFactory.withUri(
                friendlyResource.uri()
        ).getNode();
        for (Relationship relationship : getNode().getRelationships(Direction.OUTGOING)) {
            Node endNode = relationship.getEndNode();
            if (endNode.equals(friendlyResourceAsNode)) {
                relationship.delete();
            }
        }
        updateLastModificationDate();
    }

    @Override
    public Map<URI,FriendlyResource> getAdditionalTypes() {
        return getIdentificationUsingRelation(
                Relationships.TYPE
        );
    }

    @Override
    public Map<URI,FriendlyResource> getIdentifications() {
        Map<URI,FriendlyResource> identifications = getSameAs();
        identifications.putAll(getAdditionalTypes());
        identifications.putAll(getGenericIdentifications());
        return identifications;
    }

    @Override
    public void remove() {
        friendlyResource.remove();
    }

    @Override
    public String ownerUsername() {
        return UserUris.ownerUserNameFromUri(
                uri()
        );
    }

    private Map<URI, FriendlyResource> getIdentificationUsingRelation(Relationships relationship) {
        QueryResult<Map<String,Object>> result = queryEngine.query(
                queryPrefix() +
                        "MATCH " +
                        "n-[:" + relationship + "]->identification " +
                        "RETURN " +
                        "identification.uri as uri",
                map()
        );
        Iterator<Map<String, Object>> iterator = result.iterator();
        Map<URI, FriendlyResource> friendlyResources = new HashMap<>();
        while (iterator.hasNext()) {
            URI uri = URI.create(
                    iterator.next().get("uri").toString()
            );
            friendlyResources.put(
                    uri,
                    friendlyResourceFactory.withUri(
                            uri
                    )
            );
        }
        return friendlyResources;
    }

    private void ifIdentificationIsSelfThrowException(FriendlyResource identification) throws IllegalArgumentException {
        if (identification.equals(this)) {
            throw new IllegalArgumentException(
                    "identification cannot be the same"
            );
        }
    }

    private IllegalArgumentException duplicateIdentificationError() {
        return new IllegalArgumentException(
                "cannot have duplicate identifications"
        );
    }

    @Override
    public boolean equals(Object graphElementToCompare) {
        return friendlyResource.equals(graphElementToCompare);
    }

    @Override
    public int hashCode() {
        return friendlyResource.hashCode();
    }

    @Override
    public String queryPrefix() {
        return friendlyResource.queryPrefix();
    }

    @Override
    public Node getNode() {
        if (null == node) {
            node = friendlyResource.getNode();
        }
        return node;
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        return friendlyResource.addCreationProperties(
                map
        );
    }
}
