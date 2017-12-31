/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.User;
import guru.bubl.module.model.WholeGraph;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.identification.IdentificationFactory;
import guru.bubl.module.model.graph.identification.IdentificationOperator;
import guru.bubl.module.model.graph.schema.SchemaOperator;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema.SchemaFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class Neo4jWholeGraph implements WholeGraph {

    @Inject
    protected Connection connection;

    @Inject
    protected Neo4jVertexFactory neo4jVertexFactory;

    @Inject
    protected Neo4jEdgeFactory neo4jEdgeFactory;

    @Inject
    protected SchemaFactory schemaFactory;

    @Inject
    protected GraphElementOperatorFactory graphElementFactory;

    @Inject
    protected IdentificationFactory identificationFactory;

    @Override
    public Set<VertexInSubGraphOperator> getAllVertices() {
        return getAllVerticesOfUserOrNot(null);
    }

    @Override
    public Set<VertexInSubGraphOperator> getAllVerticesOfUser(User user) {
        return getAllVerticesOfUserOrNot(user);
    }

    private Set<VertexInSubGraphOperator> getAllVerticesOfUserOrNot(User user) {
        String query = String.format(
                "START n=node:node_auto_index('%s:%s%s') RETURN n.uri as uri",
                Neo4jFriendlyResource.props.type,
                GraphElementType.vertex,
                user == null ? "" : " AND owner:" + user.username()
        );
        Set<VertexInSubGraphOperator> vertices = new HashSet<>();
        return NoEx.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()) {
                vertices.add(
                        neo4jVertexFactory.withUri(
                                URI.create(
                                        rs.getString("uri")
                                )
                        )
                );
            }
            return vertices;
        }).get();
    }

    @Override
    public Set<EdgeOperator> getAllEdges() {
        return getAllEdgesOfUserOrNot(null);
    }

    @Override
    public Set<EdgeOperator> getAllEdgesOfUser(User user) {
        return getAllEdgesOfUserOrNot(user);
    }

    private Set<EdgeOperator> getAllEdgesOfUserOrNot(User user) {
        String query = String.format(
                "START n=node:node_auto_index('%s:%s%s') RETURN n.uri as uri",
                Neo4jFriendlyResource.props.type,
                GraphElementType.edge,
                user == null ? "" : " AND owner:" + user.username()
        );
        Set<EdgeOperator> edges = new HashSet<>();
        return NoEx.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()) {
                edges.add(
                        neo4jEdgeFactory.withUri(
                                URI.create(
                                        rs.getString("uri")
                                )
                        )
                );
            }
            return edges;
        }).get();
    }

    @Override
    public Set<SchemaOperator> getAllSchemas() {
        String query = String.format(
                "START n=node:node_auto_index('%s:%s') RETURN n.%s as uri",
                Neo4jFriendlyResource.props.type,
                GraphElementType.schema,
                Neo4jFriendlyResource.props.uri
        );
        Set<SchemaOperator> schemas = new HashSet<>();
        return NoEx.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()) {
                schemas.add(
                        schemaFactory.withUri(
                                URI.create(
                                        rs.getString("uri")
                                )
                        )
                );
            }
            return schemas;
        }).get();
    }

    @Override
    public Set<GraphElementOperator> getAllGraphElements() {
        return getAllGraphElementsOfUserOrNot(null);
    }

    Set<GraphElementOperator> getAllGraphElementsOfUserOrNot(User user) {
        String query = String.format(
                "START n=node:node_auto_index('( %s:%s) ') RETURN n.%s as uri",
                Neo4jFriendlyResource.props.type,
                StringUtils.join(GraphElementType.names(), " OR type:"),
                Neo4jFriendlyResource.props.uri
        );
        Set<GraphElementOperator> graphElements = new HashSet<>();
        return NoEx.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()) {
                graphElements.add(
                        graphElementFactory.withUri(
                                URI.create(
                                        rs.getString("uri")
                                )
                        )
                );
            }
            return graphElements;
        }).get();
    }

    @Override
    public Set<IdentificationOperator> getAllTags() {
        return getAllTagsOfUserOrNot(null);
    }

    @Override
    public Set<IdentificationOperator> getAllTagsOfUser(User user) {
        return getAllTagsOfUserOrNot(user);
    }

    private Set<IdentificationOperator> getAllTagsOfUserOrNot(User user) {
        String query = String.format(
                "START n=node:node_auto_index('( %s:%s%s) ') RETURN n.%s as uri",
                Neo4jFriendlyResource.props.type,
                GraphElementType.meta,
                user == null ? "" : " AND owner:" + user.username(),
                Neo4jFriendlyResource.props.uri
        );
        Set<IdentificationOperator> identifications = new HashSet<>();
        return NoEx.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            while (rs.next()) {
                identifications.add(
                        identificationFactory.withUri(
                                URI.create(
                                        rs.getString("uri")
                                )
                        )
                );
            }
            return identifications;
        }).get();
    }
}
