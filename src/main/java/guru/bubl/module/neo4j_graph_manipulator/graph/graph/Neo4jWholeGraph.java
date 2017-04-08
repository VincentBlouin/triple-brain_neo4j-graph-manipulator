/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.common_utils.NoExRun;
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
        String query = String.format(
                "START n=node:node_auto_index('%s:%s') RETURN n.uri as uri",
                Neo4jFriendlyResource.props.type,
                GraphElementType.vertex
        );
        Set<VertexInSubGraphOperator> vertices = new HashSet<>();
        return NoExRun.wrap(()->{
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            while(rs.next()){
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
        String query = String.format(
                "START n=node:node_auto_index('%s:%s') RETURN n.uri as uri",
                Neo4jFriendlyResource.props.type,
                GraphElementType.edge
        );
        Set<EdgeOperator> edges = new HashSet<>();
        return NoExRun.wrap(()->{
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            while(rs.next()){
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
        return NoExRun.wrap(()->{
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            while(rs.next()){
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
        String query = String.format(
                "START n=node:node_auto_index('( %s:%s) ') RETURN n.%s as uri",
                Neo4jFriendlyResource.props.type,
                StringUtils.join(GraphElementType.names(), " OR type:"),
                Neo4jFriendlyResource.props.uri
        );
        Set<GraphElementOperator> graphElements = new HashSet<>();
        return NoExRun.wrap(()->{
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            while(rs.next()){
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
    public Set<IdentificationOperator> getAllIdentifications() {
        String query = String.format(
                "START n=node:node_auto_index('( %s:%s) ') RETURN n.%s as uri",
                Neo4jFriendlyResource.props.type,
                GraphElementType.meta,
                Neo4jFriendlyResource.props.uri
        );
        Set<IdentificationOperator> identifications = new HashSet<>();
        return NoExRun.wrap(()->{
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            while(rs.next()){
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
