/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementFactory;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.batch.BatchCallback;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.Identification;
import guru.bubl.module.model.graph.IdentificationPojo;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;
import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.wrap;

public class Neo4jEdgeOperator implements EdgeOperator, Neo4jOperator {

    public enum props {
        source_vertex_uri,
        destination_vertex_uri
    }

    protected Node node;
    protected Neo4jGraphElementOperator graphElementOperator;
    protected Neo4jVertexFactory vertexFactory;
    protected Neo4jEdgeFactory edgeFactory;

    protected Vertex sourceVertex;
    protected Vertex destinationVertex;
    protected RestAPI restApi;
    protected QueryEngine<Map<String, Object>> queryEngine;

    @AssistedInject
    protected Neo4jEdgeOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory neo4jGraphElementFactory,
            QueryEngine queryEngine,
            RestAPI restApi,
            @Assisted Node node
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.node = node;
        this.queryEngine = queryEngine;
        this.restApi = restApi;
        graphElementOperator = neo4jGraphElementFactory.withNode(node);
    }

    @AssistedInject
    protected Neo4jEdgeOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory graphElementFactory,
            QueryEngine queryEngine,
            RestAPI restApi,
            @Assisted URI uri
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.graphElementOperator = graphElementFactory.withUri(
                uri
        );
        this.queryEngine = queryEngine;
        this.restApi = restApi;
    }

    @AssistedInject
    protected Neo4jEdgeOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory graphElementFactory,
            QueryEngine queryEngine,
            RestAPI restApi,
            @Assisted("source") Vertex sourceVertex,
            @Assisted("destination") Vertex destinationVertex
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.queryEngine = queryEngine;
        this.restApi = restApi;
        UserUris userUris = new UserUris(
                UserUris.ownerUserNameFromUri(sourceVertex.uri())
        );
        URI newEdgeUri = userUris.generateEdgeUri();
        this.graphElementOperator = graphElementFactory.withUri(
                newEdgeUri
        );
        this.sourceVertex = sourceVertex;
        this.destinationVertex = destinationVertex;
    }

    @Override
    public VertexOperator sourceVertex() {
        return vertexUsingProperty(
                props.source_vertex_uri
        );
    }

    @Override
    public VertexOperator destinationVertex() {
        return vertexUsingProperty(
                props.destination_vertex_uri
        );
    }

    private VertexOperator vertexUsingProperty(Enum prop) {
        QueryResult<Map<String, Object>> result = queryEngine.query(
                queryPrefix() +
                        "RETURN n." + prop.name(),
                map()
        );
        return vertexFactory.withUri(URI.create(
                result.iterator().next().get(
                        "n." + prop.name()
                ).toString()
        ));
    }

    @Override
    public VertexOperator otherVertex(Vertex vertex) {
        return sourceVertex().equals(vertex) ?
                destinationVertex() :
                sourceVertex();
    }

    @Override
    public boolean hasVertex(Vertex vertex) {
        return sourceVertex().equals(vertex) ||
                destinationVertex().equals(vertex);
    }

    @Override
    public void inverse() {
        restApi.executeBatch(new BatchCallback<Object>() {
            @Override
            public Object recordBatch(RestAPI restAPI) {
                queryEngine.query(
                        queryPrefix() +
                                "MATCH n-[source_rel:" + Relationships.SOURCE_VERTEX + "]->source_vertex, " +
                                "n-[destination_rel:" + Relationships.DESTINATION_VERTEX + "]->destination_vertex " +
                                "CREATE (n)-[:" + Relationships.DESTINATION_VERTEX + "]->(source_vertex) " +
                                "CREATE (n)-[:" + Relationships.SOURCE_VERTEX + "]->(destination_vertex) " +
                                "DELETE source_rel, destination_rel " +
                                "SET n." + props.source_vertex_uri + "=" + "destination_vertex.uri, " +
                                "n." + props.destination_vertex_uri + "=" + "source_vertex.uri",
                        map()
                );
                graphElementOperator.updateLastModificationDate();
                return null;
            }
        });
    }

    @Override
    public void remove() {
        restApi.executeBatch(new BatchCallback<Object>() {
            @Override
            public Object recordBatch(RestAPI restAPI) {
                queryEngine.query(
                        queryPrefix() +
                                "MATCH " +
                                "n-[:SOURCE_VERTEX]->(source_vertex), " +
                                "n-[:DESTINATION_VERTEX]->(destination_vertex) " +
                                "SET source_vertex.number_of_connected_edges_property_name = " +
                                "source_vertex.number_of_connected_edges_property_name - 1, " +
                                "destination_vertex.number_of_connected_edges_property_name = " +
                                "destination_vertex.number_of_connected_edges_property_name - 1 ",
                        map()
                );
                queryEngine.query(
                        queryPrefix() +
                                "MATCH " +
                                "n-[r]-() " +
                                "DELETE r, n",
                        map()
                );
                return null;
            }
        });
    }

    @Override
    public Date creationDate() {
        return graphElementOperator.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return graphElementOperator.lastModificationDate();
    }

    @Override
    public String getOwnerUsername() {
        return graphElementOperator.getOwnerUsername();
    }

    @Override
    public URI uri() {
        return graphElementOperator.uri();
    }

    @Override
    public String label() {
        return graphElementOperator.label();
    }

    @Override
    public void label(String label) {
        graphElementOperator.label(label);
    }

    @Override
    public Set<Image> images() {
        return graphElementOperator.images();
    }

    @Override
    public Boolean gotImages() {
        return graphElementOperator.gotImages();
    }

    @Override
    public String comment() {
        return graphElementOperator.comment();
    }

    @Override
    public void comment(String comment) {
        graphElementOperator.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return graphElementOperator.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        graphElementOperator.addImages(images);
    }

    @Override
    public boolean hasLabel() {
        return graphElementOperator.hasLabel();
    }

    @Override
    public IdentificationPojo addGenericIdentification(Identification friendlyResource) {
        return graphElementOperator.addGenericIdentification(
                friendlyResource
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
        String query = "START source_node=node:node_auto_index(\"uri:" + sourceVertex.uri() + "\"), " +
                "destination_node=node:node_auto_index(\"uri:" + destinationVertex.uri() + "\") " +
                "create (n:" + GraphElementType.edge + " {props}), " +
                "n-[:" + Relationships.SOURCE_VERTEX.name() + "]->source_node, " +
                "n-[:" + Relationships.DESTINATION_VERTEX.name() + "]->destination_node " +
                "return n, source_node, destination_node";
        values = addCreationProperties(values);
        queryEngine.query(
                query,
                wrap(values)
        );
    }

    @Override
    public Map<URI, Identification> getGenericIdentifications() {
        return graphElementOperator.getGenericIdentifications();
    }

    @Override
    public IdentificationPojo addSameAs(Identification friendlyResourceImpl) {
        return graphElementOperator.addSameAs(friendlyResourceImpl);
    }

    @Override
    public Map<URI, Identification> getSameAs() {
        return graphElementOperator.getSameAs();
    }

    @Override
    public IdentificationPojo addType(Identification type) {
        return graphElementOperator.addType(type);
    }

    @Override
    public void removeIdentification(Identification type) {
        graphElementOperator.removeIdentification(type);
    }

    @Override
    public Map<URI, Identification> getAdditionalTypes() {
        return graphElementOperator.getAdditionalTypes();
    }

    @Override
    public Map<URI, IdentificationPojo> getIdentifications() {
        return graphElementOperator.getIdentifications();
    }

    @Override
    public boolean equals(Object edgeToCompareAsObject) {
        return graphElementOperator.equals(edgeToCompareAsObject);
    }

    @Override
    public int hashCode() {
        return graphElementOperator.hashCode();
    }

    @Override
    public String queryPrefix() {
        return graphElementOperator.queryPrefix();
    }

    @Override
    public Node getNode() {
        if (null == node) {
            return graphElementOperator.getNode();
        }
        return node;
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Map<String, Object> newMap = map(
                props.source_vertex_uri.name(), sourceVertex.uri().toString(),
                props.destination_vertex_uri.name(), destinationVertex.uri().toString(),
                Neo4jFriendlyResource.props.type.name(), GraphElementType.edge.name()
        );
        newMap.putAll(
                map
        );
        newMap = graphElementOperator.addCreationProperties(
                newMap
        );
        return newMap;
    }

    @Override
    public URI getExternalResourceUri() {
        return graphElementOperator.getExternalResourceUri();
    }
}
