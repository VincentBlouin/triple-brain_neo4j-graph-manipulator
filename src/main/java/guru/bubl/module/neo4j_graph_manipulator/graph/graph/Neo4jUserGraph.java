/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.exceptions.InvalidDepthOfSubVerticesException;
import guru.bubl.module.model.graph.exceptions.NonExistingResourceException;
import guru.bubl.module.model.graph.schema.SchemaOperator;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.graph.vertex.VertexPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema.Neo4jSchemaExtractorFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.Neo4jSubGraphExtractorFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema.SchemaFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;

import javax.inject.Inject;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Neo4jUserGraph implements UserGraph {

    public static final String URI_PROPERTY_NAME = "uri";

    private User user;
    private Neo4jVertexFactory vertexFactory;
    private SchemaFactory schemaFactory;
    private Neo4jSubGraphExtractorFactory subGraphExtractorFactory;
    private Neo4jEdgeFactory edgeFactory;
    private Neo4jSchemaExtractorFactory schemaExtractorFactory;

    @Inject
    Connection connection;

    @AssistedInject
    protected Neo4jUserGraph(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jSubGraphExtractorFactory subGraphExtractorFactory,
            Neo4jSchemaExtractorFactory schemaExtractorFactory,
            SchemaFactory schemaFactory,
            @Assisted User user
    ) {
        this.user = user;
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.subGraphExtractorFactory = subGraphExtractorFactory;
        this.schemaExtractorFactory = schemaExtractorFactory;
        this.schemaFactory = schemaFactory;
    }

    @Override
    public VertexOperator defaultVertex() {
        return getAnyVertex();
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Boolean haveElementWithId(URI uri) {
        String query = String.format(
                "START n=node:node_auto_index('uri:%s') return n.uri as uri",
                uri
        );
        return NoExRun.wrap(() ->
                connection.createStatement().executeQuery(
                        query
                ).next()
        ).get();
    }

    @Override
    public SubGraphPojo graphWithDepthAndCenterBubbleUri(Integer depthOfSubVertices, URI centerBubbleUri) throws NonExistingResourceException {
        return graphWithDepthResultsLimitAndCenterBubbleUri(depthOfSubVertices, null, centerBubbleUri);
    }

    @Override
    public SubGraphPojo graphWithDepthResultsLimitAndCenterBubbleUri(Integer depthOfSubVertices, Integer resultsLimit, URI centerBubbleUri) throws NonExistingResourceException {
        if (depthOfSubVertices < 0) {
            throw new InvalidDepthOfSubVerticesException(
                    depthOfSubVertices,
                    centerBubbleUri
            );
        }
        SubGraphPojo subGraph = resultsLimit == null ? subGraphExtractorFactory.withCenterVertexAndDepth(
                centerBubbleUri,
                depthOfSubVertices
        ).load() : subGraphExtractorFactory.withCenterVertexDepthAndResultsLimit(
                centerBubbleUri,
                depthOfSubVertices,
                resultsLimit
        ).load();
        if (subGraph.vertices().isEmpty() && !UserUris.isUriOfAnIdentifier(centerBubbleUri)) {
            throw new NonExistingResourceException(
                    centerBubbleUri
            );
        }
        return subGraph;
    }

    @Override
    public SubGraphPojo graphWithAnyVertexAndDepth(Integer depth) throws InvalidDepthOfSubVerticesException {
        return graphWithDepthAndCenterBubbleUri(
                depth,
                getAnyVertex().uri()
        );
    }

    @Override
    public VertexOperator vertexWithUri(URI uri) {
        return vertexFactory.withUri(
                uri
        );
    }

    @Override
    public EdgeOperator edgeWithUri(URI uri) {
        return edgeFactory.withUri(
                uri
        );
    }

    @Override
    public SchemaPojo schemaPojoWithUri(URI uri) {
        return schemaExtractorFactory.havingUri(
                uri
        ).load();
    }

    @Override
    public SchemaOperator schemaOperatorWithUri(URI uri) {
        return schemaFactory.withUri(uri);
    }

    @Override
    public VertexPojo createVertex() {
        VertexOperator operator = vertexFactory.createForOwnerUsername(
                user.username()
        );
        return new VertexPojo(
                operator.uri()
        );
    }

    @Override
    public SchemaPojo createSchema() {
        SchemaOperator schemaOperator = schemaFactory.createForOwnerUsername(
                user.username()
        );
        return new SchemaPojo(
                schemaOperator.uri()
        );
    }

    private VertexOperator getAnyVertex() {
        String query = String.format(
                "START n=node:node_auto_index('type:%s AND owner:%s') return n.uri limit 1",
                GraphElementType.vertex,
                user.username()
        );
        URI uri = NoExRun.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(
                    query
            );
            rs.next();
            return URI.create(
                    rs.getString("n.uri")
            );
        }).get();
        return vertexFactory.withUri(uri);
    }
}
