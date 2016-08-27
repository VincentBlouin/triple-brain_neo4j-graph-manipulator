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
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.identification.Identification;
import guru.bubl.module.model.graph.identification.IdentificationPojo;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

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

    @AssistedInject
    protected Neo4jEdgeOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory graphElementFactory,
            @Assisted URI uri,
            @Assisted("source") Vertex sourceVertex,
            @Assisted("destination") Vertex destinationVertex
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.graphElementOperator = graphElementFactory.withUri(
                uri
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
    public void changeSourceVertex(
            Vertex newSourceVertex
    ) {
        changeEndVertex(
                newSourceVertex,
                Relationships.SOURCE_VERTEX
        );
    }

    @Override
    public void changeDestinationVertex(
            Vertex newDestinationVertex
    ) {
        changeEndVertex(
                newDestinationVertex,
                Relationships.DESTINATION_VERTEX
        );
    }

    private void changeEndVertex(
            Vertex newEndVertex,
            Relationships relationshipToChange
    ) {
        Relationships relationshipToKeep = Relationships.SOURCE_VERTEX == relationshipToChange ?
                Relationships.DESTINATION_VERTEX : Relationships.SOURCE_VERTEX;
        String query = String.format(
                "%s, new_v=node:node_auto_index('%s:%s') " +
                        "MATCH n-[prev_rel:%s]->prev_v, " +
                        "n-[:%s]->kept_v " +
                        "CREATE (n)-[:%s]->(new_v) " +
                        "DELETE prev_rel " +
                        "SET n.%s=new_v.uri, " +
                        "prev_v.%s = prev_v.%s - 1, " +
                        "new_v.%s = new_v.%s + 1, " +
                        "kept_v.%s = CASE WHEN (not prev_v.%s AND new_v.%s) THEN kept_v.%s + 1 ELSE kept_v.%s END, " +
                        "kept_v.%s = CASE WHEN (prev_v.%s AND not new_v.%s) THEN kept_v.%s - 1 ELSE kept_v.%s END, " +
                        "prev_v.%s = CASE WHEN kept_v.%s THEN prev_v.%s - 1 ELSE prev_v.%s END, " +
                        "new_v.%s = CASE WHEN kept_v.%s THEN new_v.%s + 1 ELSE new_v.%s END ",
                queryPrefix(),
                Neo4jFriendlyResource.props.uri,
                newEndVertex.uri(),
                relationshipToChange,
                relationshipToKeep,
                relationshipToChange,
                Relationships.SOURCE_VERTEX == relationshipToChange ? props.source_vertex_uri : props.destination_vertex_uri,
                Neo4jVertexInSubGraphOperator.props.number_of_connected_edges_property_name,
                Neo4jVertexInSubGraphOperator.props.number_of_connected_edges_property_name,
                Neo4jVertexInSubGraphOperator.props.number_of_connected_edges_property_name,
                Neo4jVertexInSubGraphOperator.props.number_of_connected_edges_property_name,
                Neo4jVertexInSubGraphOperator.props.nb_public_neighbors,
                Neo4jVertexInSubGraphOperator.props.is_public,
                Neo4jVertexInSubGraphOperator.props.is_public,
                Neo4jVertexInSubGraphOperator.props.nb_public_neighbors,
                Neo4jVertexInSubGraphOperator.props.nb_public_neighbors,
                Neo4jVertexInSubGraphOperator.props.nb_public_neighbors,
                Neo4jVertexInSubGraphOperator.props.is_public,
                Neo4jVertexInSubGraphOperator.props.is_public,
                Neo4jVertexInSubGraphOperator.props.nb_public_neighbors,
                Neo4jVertexInSubGraphOperator.props.nb_public_neighbors,
                Neo4jVertexInSubGraphOperator.props.nb_public_neighbors,
                Neo4jVertexInSubGraphOperator.props.is_public,
                Neo4jVertexInSubGraphOperator.props.nb_public_neighbors,
                Neo4jVertexInSubGraphOperator.props.nb_public_neighbors,
                Neo4jVertexInSubGraphOperator.props.nb_public_neighbors,
                Neo4jVertexInSubGraphOperator.props.is_public,
                Neo4jVertexInSubGraphOperator.props.nb_public_neighbors,
                Neo4jVertexInSubGraphOperator.props.nb_public_neighbors

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
    public EdgeOperator forkUsingSourceAndDestinationVertexAndCache(
            Vertex sourceVertex,
            Vertex destinationVertex,
            Edge cache
    ) {
        EdgeOperator clone = edgeFactory.withSourceAndDestinationVertex(
                sourceVertex,
                destinationVertex
        );
        graphElementOperator.forkUsingCreationPropertiesAndCache(
                clone,
                map(),
                cache
        );
        return clone;
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
                            "%s" +
                                    "MATCH n-[:SOURCE_VERTEX]->(s_v), n-[:DESTINATION_VERTEX]->(d_v) " +
                                    "SET s_v.number_of_connected_edges_property_name = s_v.number_of_connected_edges_property_name - 1, " +
                                    "d_v.number_of_connected_edges_property_name = d_v.number_of_connected_edges_property_name - 1," +
                                    "s_v.nb_public_neighbors = CASE WHEN d_v.is_public THEN s_v.nb_public_neighbors - 1 ELSE s_v.nb_public_neighbors END, " +
                                    "d_v.nb_public_neighbors = CASE WHEN s_v.is_public THEN d_v.nb_public_neighbors - 1 ELSE d_v.nb_public_neighbors END ",
                            queryPrefix()
                    )
            );
            graphElementOperator.removeAllIdentifications();
            return connection.createStatement().executeQuery(
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
    public Map<URI, IdentificationPojo> addGenericIdentification(Identification friendlyResource) {
        return graphElementOperator.addGenericIdentification(
                friendlyResource
        );
    }

    @Override
    public void setSortDate(Date sortDate, Date moveDate) {
        graphElementOperator.setSortDate(
                sortDate,
                moveDate
        );
    }

    @Override
    public void create() {
        createUsingInitialValues(
                map()
        );
    }

    @Override
    public EdgePojo createEdge() {
        return createEdgeUsingInitialValues(
                map()
        );
    }

    @Override
    public EdgePojo createEdgeUsingInitialValues(Map<String, Object> values) {
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
        return NoExRun.wrap(() -> {
            PreparedStatement statement = connection.prepareStatement(
                    query
            );
            Map<String, Object> creationProperties = addCreationProperties(values);
            statement.setObject(
                    1,
                    creationProperties
            );
            statement.execute();
            statement.close();
            EdgePojo edge = new EdgePojo(
                    graphElementOperator.pojoFromCreationProperties(
                            creationProperties
                    )
            );
            edge.setSourceVertex(new VertexInSubGraphPojo(
                    sourceVertex.uri()
            ));
            edge.setDestinationVertex(new VertexInSubGraphPojo(
                    destinationVertex.uri()
            ));
            return edge;
        }).get();
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        createEdgeUsingInitialValues(values);
    }

    @Override
    public Map<URI, IdentificationPojo> getGenericIdentifications() {
        return graphElementOperator.getGenericIdentifications();
    }

    @Override
    public Map<URI, IdentificationPojo> addSameAs(Identification friendlyResourceImpl) {
        return graphElementOperator.addSameAs(friendlyResourceImpl);
    }

    @Override
    public Map<URI, IdentificationPojo> getSameAs() {
        return graphElementOperator.getSameAs();
    }

    @Override
    public Map<URI, IdentificationPojo> addType(Identification type) {
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
