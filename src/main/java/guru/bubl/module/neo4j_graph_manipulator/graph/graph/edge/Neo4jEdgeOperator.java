/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoExRun;
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
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
import org.neo4j.graphdb.Node;

import javax.inject.Inject;
import java.net.URI;
import java.sql.*;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

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

    @Inject
    protected
    Connection connection;

    @AssistedInject
    protected Neo4jEdgeOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory graphElementFactory,
            @Assisted URI uri
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.graphElementOperator = graphElementFactory.withUri(
                uri
        );
    }

    @AssistedInject
    protected Neo4jEdgeOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory graphElementFactory,
            @Assisted("source") Vertex sourceVertex,
            @Assisted("destination") Vertex destinationVertex
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
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
        return NoExRun.wrap(() -> {
            String query = String.format(
                    "%sRETURN n.%s",
                    queryPrefix(),
                    prop.name()
            );
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            rs.next();
            return vertexFactory.withUri(URI.create(
                    rs.getString(
                            "n." + prop.name()
                    )
            ));
        }).get();
    }

    @Override
    public VertexOperator otherVertex(Vertex vertex) {
        return sourceVertex().equals(vertex) ?
                destinationVertex() :
                sourceVertex();
    }

    @Override
    public void changeSourceVertex(Vertex vertex) {
        String query = String.format(
                "%s, new_source_vertex=node:node_auto_index('%s:%s') " +
                        "MATCH n-[prev_source_rel:%s]->prev_source_vertex " +
                        "CREATE (n)-[:%s]->(new_source_vertex) " +
                        "DELETE prev_source_rel " +
                        "SET n.%s=new_source_vertex.uri, " +
                        "prev_source_vertex.%s = prev_source_vertex.%s - 1, " +
                        "new_source_vertex.%s = new_source_vertex.%s + 1 ",
                queryPrefix(),
                Neo4jFriendlyResource.props.uri,
                vertex.uri(),
                Relationships.SOURCE_VERTEX,
                Relationships.SOURCE_VERTEX,
                props.source_vertex_uri,
                Neo4jVertexInSubGraphOperator.props.number_of_connected_edges_property_name,
                Neo4jVertexInSubGraphOperator.props.number_of_connected_edges_property_name,
                Neo4jVertexInSubGraphOperator.props.number_of_connected_edges_property_name,
                Neo4jVertexInSubGraphOperator.props.number_of_connected_edges_property_name
        );
        //todo batch
        NoExRun.wrap(() ->
                        connection.createStatement().executeQuery(
                                query
                        )
        ).get();
        graphElementOperator.updateLastModificationDate();
        //todo endbatch
    }


    @Override
    public void inverse() {
        String query = String.format(
                "%sMATCH n-[source_rel:%s]->source_vertex, " +
                        "n-[destination_rel:%s]->destination_vertex " +
                        "CREATE (n)-[:%s]->(source_vertex) " +
                        "CREATE (n)-[:%s]->(destination_vertex) " +
                        "DELETE source_rel, destination_rel " +
                        "SET n.%s=destination_vertex.uri, n.%s=source_vertex.uri",
                queryPrefix(),
                Relationships.SOURCE_VERTEX,
                Relationships.DESTINATION_VERTEX,
                Relationships.DESTINATION_VERTEX,
                Relationships.SOURCE_VERTEX,
                props.source_vertex_uri,
                props.destination_vertex_uri
        );
        //todo batch
        NoExRun.wrap(() ->
                        connection.createStatement().executeQuery(
                                query
                        )
        ).get();
        graphElementOperator.updateLastModificationDate();
        //todo endbatch

    }

    @Override
    public void remove() {
        //todo batch
        NoExRun.wrap(() -> {
            Statement statement = connection.createStatement();
            statement.executeQuery(
                    String.format(
                            "%sMATCH n-[:SOURCE_VERTEX]->(source_vertex), n-[:DESTINATION_VERTEX]->(destination_vertex) " +
                                    "SET source_vertex.number_of_connected_edges_property_name = source_vertex.number_of_connected_edges_property_name - 1, " +
                                    "destination_vertex.number_of_connected_edges_property_name = destination_vertex.number_of_connected_edges_property_name - 1 ",
                            queryPrefix()
                    )
            );
            statement = connection.createStatement();
            return statement.executeQuery(
                    String.format(
                            "%sMATCH n-[r]-() DELETE r, n",
                            queryPrefix()
                    )
            );
        }).get();
        //todo endbatch
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
        String query = String.format(
                "START source_node=node:node_auto_index(\"uri:%s\"), " +
                        "destination_node=node:node_auto_index(\"uri:%s\") " +
                        "CREATE (n:%s {1}), n-[:%s]->source_node, n-[:%s]->destination_node " +
                        "SET n.%s = source_node.%s AND destination_node.%s " +
                        "RETURN n, source_node, destination_node",
                sourceVertex.uri(),
                destinationVertex.uri(),
                GraphElementType.edge,
                Relationships.SOURCE_VERTEX.name(),
                Relationships.DESTINATION_VERTEX.name(),
                Neo4jVertexInSubGraphOperator.props.is_public,
                Neo4jVertexInSubGraphOperator.props.is_public,
                Neo4jVertexInSubGraphOperator.props.is_public
        );
        NoExRun.wrap(() -> {
            PreparedStatement statement = connection.prepareStatement(
                    query
            );
            statement.setObject(
                    1,
                    addCreationProperties(values)
            );
            return statement.execute();
        }).get();
    }

    @Override
    public Map<URI, IdentificationPojo> getGenericIdentifications() {
        return graphElementOperator.getGenericIdentifications();
    }

    @Override
    public IdentificationPojo addSameAs(Identification friendlyResourceImpl) {
        return graphElementOperator.addSameAs(friendlyResourceImpl);
    }

    @Override
    public Map<URI, IdentificationPojo> getSameAs() {
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
    public Map<URI, IdentificationPojo> getAdditionalTypes() {
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
    public void setNamedCreationProperties(NamedParameterStatement statement) throws SQLException {
        statement.setString(
                props.source_vertex_uri.name(),
                sourceVertex.uri().toString()
        );
        statement.setString(
                props.destination_vertex_uri.name(),
                destinationVertex.uri().toString()
        );
        statement.setString(
                Neo4jFriendlyResource.props.type.name(),
                GraphElementType.edge.name()
        );
        graphElementOperator.setNamedCreationProperties(statement);
    }

    @Override
    public Boolean isPublic() {
        String query = String.format(
                "%s return n.%s as is_public",
                queryPrefix(),
                Neo4jVertexInSubGraphOperator.props.is_public
        );
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            return rs.getBoolean("is_public");
        }).get();
    }
}
