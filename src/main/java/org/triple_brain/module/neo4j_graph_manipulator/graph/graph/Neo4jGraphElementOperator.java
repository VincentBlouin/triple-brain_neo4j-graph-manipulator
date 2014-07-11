package org.triple_brain.module.neo4j_graph_manipulator.graph.graph;

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
import org.triple_brain.module.model.graph.*;
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
    protected Neo4jIdentification identification;
    protected URI uri;
    protected QueryEngine<Map<String, Object>> queryEngine;
    protected RestAPI restApi;
    protected Neo4jIdentificationFactory identificationFactory;

    @Inject
    protected Neo4jUtils neo4jUtils;

    @AssistedInject
    protected Neo4jGraphElementOperator(
            QueryEngine queryEngine,
            RestAPI restApi,
            Neo4jIdentificationFactory identificationFactory,
            @Assisted Node node
    ) {
        identification = identificationFactory.withNode(
                node
        );
        this.identificationFactory = identificationFactory;
        this.node = node;
        this.queryEngine = queryEngine;
        this.restApi = restApi;
    }

    @AssistedInject
    protected Neo4jGraphElementOperator(
            Neo4jUtils utils,
            QueryEngine queryEngine,
            RestAPI restApi,
            Neo4jIdentificationFactory identificationFactory,
            @Assisted URI uri
    ) {
        identification = identificationFactory.withUri(
                uri
        );
        this.identificationFactory = identificationFactory;
        this.neo4jUtils = utils;
        this.queryEngine = queryEngine;
        this.restApi = restApi;
    }

    @Override
    public Date creationDate() {
        return identification.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return identification.lastModificationDate();
    }

    @Override
    public String getOwner() {
        return identification.getOwner();
    }

    public void updateLastModificationDate() {
        identification.updateLastModificationDate();
    }

    @Override
    public URI uri() {
        return identification.uri();
    }

    @Override
    public String label() {
        return identification.label();
    }

    @Override
    public void label(String label) {
        identification.label(
                label
        );
    }

    @Override
    public Set<Image> images() {
        return identification.images();
    }

    @Override
    public Boolean gotImages() {
        return identification.gotImages();
    }

    @Override
    public String comment() {
        return identification.comment();
    }

    @Override
    public void comment(String comment) {
        identification.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return identification.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        identification.addImages(
                images
        );
    }

    @Override
    public boolean hasLabel() {
        return identification.hasLabel();
    }

    @Override
    public IdentificationPojo addGenericIdentification(Identification genericIdentification) throws IllegalArgumentException {
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
        identification.createUsingInitialValues(values);
    }

    @Override
    public Map<URI, Identification> getGenericIdentifications() {
        return getIdentificationUsingRelation(
                Relationships.IDENTIFIED_TO
        );
    }

    @Override
    public IdentificationPojo addSameAs(Identification sameAs) throws IllegalArgumentException {
        return addIdentificationUsingRelation(
                sameAs,
                Relationships.SAME_AS
        );
    }

    private IdentificationPojo addIdentificationUsingRelation(
            final Identification identification,
            final Relationships relation
    ) {
        ifIdentificationIsSelfThrowException(identification);
        final Neo4jIdentification neo4jIdentification = identificationFactory.withUri(
                new UserUris(getOwner()).generateIdentificationUri()
        );
        final String queryPrefix = this.identification.queryPrefix();
        QueryResult<Map<String, Object>> results = restApi.executeBatch(new BatchCallback<QueryResult<Map<String, Object>>>() {
            @Override
            public QueryResult<Map<String, Object>> recordBatch(RestAPI restApi) {
                QueryResult<Map<String, Object>> results = queryEngine.query(
                        queryPrefix +
                                "MERGE (f {" +
                                Neo4jIdentification.props.external_uri + ": {external_uri}, " +
                                Neo4jFriendlyResource.props.owner + ": {owner}" +
                                "}) " +
                                "ON CREATE SET " +
                                "f.uri = {uri}, " +
                                "f.`" + RDFS.label.getURI() + "`={label}, " +
                                "f.`" + RDFS.comment.getURI() + "`={comment}, " +
                                "f." + Neo4jFriendlyResource.props.creation_date + "=timestamp(), " +
                                "f." + Neo4jFriendlyResource.props.last_modification_date + "=timestamp() " +
                                "CREATE UNIQUE " +
                                "n-[:" + relation + "]->f " +
                                "RETURN " +
                                "f.uri as uri, " +
                                "f.external_uri as external_uri, " +
                                "f.`" + RDFS.label.getURI() + "` as label, " +
                                "f.`" + RDFS.comment.getURI() + "` as comment, " +
                                "f." + Neo4jFriendlyResource.props.creation_date + " as creation_date, " +
                                "f." + Neo4jFriendlyResource.props.last_modification_date + " as last_modification_date",
                        neo4jIdentification.addCreationProperties(
                                map(
                                        "label",
                                        identification.label(),
                                        "comment",
                                        identification.comment(),
                                        "external_uri",
                                        identification.getExternalResourceUri().toString()
                                )
                        )
                );
                updateLastModificationDate();
                return results;
            }
        });
        Map<String, Object> result = results.iterator().next();
        return new IdentificationPojo(
                URI.create(
                        result.get("external_uri").toString()
                ),
                new FriendlyResourcePojo(
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
                )
        );
    }

    @Override
    public Map<URI, Identification> getSameAs() {
        return getIdentificationUsingRelation(
                Relationships.SAME_AS
        );
    }

    @Override
    public IdentificationPojo addType(Identification type) throws IllegalArgumentException {
        return addIdentificationUsingRelation(
                type,
                Relationships.TYPE
        );
    }

    @Override
    public void removeIdentification(Identification friendlyResource) {
        Node identificationAsNode = identificationFactory.withUri(
                friendlyResource.uri()
        ).getNode();
        for (Relationship relationship : getNode().getRelationships(Direction.OUTGOING)) {
            Node endNode = relationship.getEndNode();
            if (endNode.equals(identificationAsNode)) {
                relationship.delete();
            }
        }
        updateLastModificationDate();
    }

    @Override
    public Map<URI, Identification> getAdditionalTypes() {
        return getIdentificationUsingRelation(
                Relationships.TYPE
        );
    }

    @Override
    public Map<URI, Identification> getIdentifications() {
        Map<URI, Identification> identifications = getSameAs();
        identifications.putAll(getAdditionalTypes());
        identifications.putAll(getGenericIdentifications());
        return identifications;
    }

    @Override
    public void remove() {
        identification.remove();
    }

    @Override
    public String ownerUsername() {
        return UserUris.ownerUserNameFromUri(
                uri()
        );
    }

    private Map<URI, Identification> getIdentificationUsingRelation(Relationships relationship) {
        QueryResult<Map<String, Object>> result = queryEngine.query(
                queryPrefix() +
                        "MATCH " +
                        "n-[:" + relationship + "]->identification " +
                        "RETURN " +
                        "identification.uri as uri",
                map()
        );
        Iterator<Map<String, Object>> iterator = result.iterator();
        Map<URI, Identification> identifications = new HashMap<>();
        while (iterator.hasNext()) {
            URI uri = URI.create(
                    iterator.next().get("uri").toString()
            );
            identifications.put(
                    uri,
                    identificationFactory.withUri(
                            uri
                    )
            );
        }
        return identifications;
    }

    private void ifIdentificationIsSelfThrowException(Identification identification) throws IllegalArgumentException {
        if (identification.getExternalResourceUri().equals(this.uri())) {
            throw new IllegalArgumentException(
                    "identification cannot be the same"
            );
        }
    }

    @Override
    public boolean equals(Object graphElementToCompare) {
        return identification.equals(graphElementToCompare);
    }

    @Override
    public int hashCode() {
        return identification.hashCode();
    }

    @Override
    public String queryPrefix() {
        return identification.queryPrefix();
    }

    @Override
    public Node getNode() {
        if (null == node) {
            node = identification.getNode();
        }
        return node;
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        return identification.addCreationProperties(
                map
        );
    }

    @Override
    public URI getExternalResourceUri() {
        return identification.getExternalResourceUri();
    }
}
