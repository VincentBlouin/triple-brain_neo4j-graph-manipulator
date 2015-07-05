/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.json.ImageJson;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jIdentificationFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.Neo4jImages;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.batch.BatchCallback;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.json.IdentificationJson;

import java.net.URI;
import java.util.*;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class Neo4jGraphElementOperator implements GraphElementOperator, Neo4jOperator {

    public enum props{
        identifications
    }

    protected Node node;
    protected Neo4jIdentification identification;
    protected URI uri;
    protected QueryEngine<Map<String, Object>> queryEngine;
    protected RestAPI restApi;
    protected Neo4jIdentificationFactory identificationFactory;

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
            QueryEngine queryEngine,
            RestAPI restApi,
            Neo4jIdentificationFactory identificationFactory,
            @Assisted URI uri
    ) {
        identification = identificationFactory.withUri(
                uri
        );
        this.identificationFactory = identificationFactory;
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
    public String getOwnerUsername() {
        return identification.getOwnerUsername();
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
        return addIdentificationUsingType(
                genericIdentification,
                IdentificationType.generic
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
        return getIdentificationsUsingRelation(
                IdentificationType.generic
        );
    }

    @Override
    public IdentificationPojo addSameAs(Identification sameAs) throws IllegalArgumentException {
        return addIdentificationUsingType(
                sameAs,
                IdentificationType.same_as
        );
    }

    private IdentificationPojo addIdentificationUsingType(
            Identification identification,
            IdentificationType identificationType
    ) {
        ifIdentificationIsSelfThrowException(identification);
        IdentificationPojo identificationPojo = new IdentificationPojo(
                new UserUris(getOwnerUsername()).generateIdentificationUri(),
                identification
        );
        identificationPojo.setCreationDate(new Date());
        identificationPojo.setType(
                identificationType
        );
        final Neo4jIdentification neo4jIdentification = identificationFactory.withUri(
                new UserUris(getOwnerUsername()).generateIdentificationUri()
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
                                "f." + Neo4jFriendlyResource.props.type + "={type}, " +
                                "f." + Neo4jFriendlyResource.props.label + "={label}, " +
                                "f." + Neo4jFriendlyResource.props.comment + "={comment}, " +
                                "f." + Neo4jImages.props.images + "={images}, " +
                                "f." + Neo4jFriendlyResource.props.creation_date + "=timestamp(), " +
                                "f." + Neo4jFriendlyResource.props.last_modification_date + "=timestamp() " +
                                "CREATE UNIQUE " +
                                "n-[r:" + Relationships.IDENTIFIED_TO + "]->f " +
                                "SET r.type={type}" +
                                "RETURN " +
                                "f.uri as uri, " +
                                "f.external_uri as external_uri, " +
                                "f." + Neo4jFriendlyResource.props.label + " as label, " +
                                "f." + Neo4jFriendlyResource.props.comment + " as comment, " +
                                "f." + Neo4jImages.props.images + " as images, " +
                                "f." + Neo4jFriendlyResource.props.creation_date + " as creation_date, " +
                                "f." + Neo4jFriendlyResource.props.last_modification_date + " as last_modification_date",
                        neo4jIdentification.addCreationProperties(
                                map(
                                        "label",
                                        identification.label(),
                                        "comment",
                                        identification.comment(),
                                        Neo4jImages.props.images.name(),
                                        ImageJson.toJsonArray(identification.images()),
                                        "external_uri",
                                        identification.getExternalResourceUri().toString(),
                                        "type",
                                        identificationType.name()
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
                        result.get("images") == null ?
                                new HashSet<>() : ImageJson.fromJson(result.get("images").toString()),
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
        return getIdentificationsUsingRelation(
                IdentificationType.same_as
        );
    }

    @Override
    public IdentificationPojo addType(Identification type) throws IllegalArgumentException {
        return addIdentificationUsingType(
                type,
                IdentificationType.type
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
        return getIdentificationsUsingRelation(
                IdentificationType.type
        );
    }

    @Override
    public void remove() {
        identification.remove();
    }


    private Map<URI, Identification> getIdentificationsUsingRelation(IdentificationType identificationType) {
        QueryResult<Map<String, Object>> results = queryEngine.query(
                queryPrefix() +
                        "MATCH " +
                        "n-[r:" + Relationships.IDENTIFIED_TO + "]->identification " +
                        "WHERE r.type='" + identificationType + "' " +
                        "RETURN " +
                        "identification.uri as uri, " +
                        "identification.external_uri as external_uri",
                map()
        );
        Iterator<Map<String, Object>> iterator = results.iterator();
        Map<URI, Identification> identifications = new HashMap<>();
        while (iterator.hasNext()) {
            Map<String, Object> result = iterator.next();
            URI uri = URI.create(
                    result.get("uri").toString()
            );
            URI externalUri = URI.create(
                    result.get("external_uri").toString()
            );
            identifications.put(
                    externalUri,
                    new IdentificationPojo(
                            externalUri,
                            new FriendlyResourcePojo(
                                    uri
                            )
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

    @Override
    public Map<URI, IdentificationPojo> getIdentifications() {
        QueryResult<Map<String, Object>> results = queryEngine.query(
                queryPrefix() +
                        "MATCH " +
                        "n-[r:" + Relationships.IDENTIFIED_TO + "]->identification " +
                        "RETURN " +
                        "identification.uri as uri, identification.external_uri as external_uri, r.type as type",
                map()
        );
        Iterator<Map<String, Object>> iterator = results.iterator();
        Map<URI, IdentificationPojo> identifications = new HashMap<>();
        while (iterator.hasNext()) {
            Map<String, Object> result = iterator.next();
            URI uri = URI.create(
                    result.get("uri").toString()
            );
            URI externalUri = URI.create(
                    result.get("external_uri").toString()
            );
            IdentificationPojo identification = new IdentificationPojo(
                    externalUri,
                    new FriendlyResourcePojo(
                            uri
                    )
            );
            identification.setType(
                    IdentificationType.valueOf(
                            result.get("type").toString()
                    )
            );
            identifications.put(
                    externalUri,
                    identification
            );
        }
        return identifications;
    }

    private void setIdentifications(Map<URI, IdentificationPojo> identifications){
        queryEngine.query(
                this.identification.queryPrefix() +
                        "SET n." + props.identifications + "= { " + props.identifications + "} ",
                map(
                        props.identifications.name(), IdentificationJson.toJson(identifications)
                )
        );
    }
}
