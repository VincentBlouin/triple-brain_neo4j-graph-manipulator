/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import com.google.inject.Inject;
import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.FriendlyResource;
import guru.bubl.module.model.graph.GraphElement;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.json.JsonUtils;
import guru.bubl.module.model.search.GraphIndexer;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.Neo4jSubGraphExtractorFactory;

import java.net.URI;
import java.sql.Connection;
import java.util.*;

public class Neo4jGraphIndexer implements GraphIndexer {

    @Inject
    Neo4jSubGraphExtractorFactory subGraphExtractorFactory;

    @Inject
    Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory;

    @Inject
    Connection connection;

    @Override
    public void indexVertex(VertexOperator vertex) {
        SubGraphPojo subGraph = subGraphExtractorFactory.withCenterVertexDepthAndResultsLimit(
                vertex.uri(),
                1,
                10
        ).load();
        subGraph.vertices().remove(vertex.uri());
        setPrivateAndPublicSearchContextToFriendlyResource(
                subGraph.vertices(),
                subGraph.getPublicVertices(),
                vertex
        );
    }

    @Override
    public void indexRelation(Edge edge) {
        Map<URI, Vertex> sourceAndDestination = new HashMap<>();
        Vertex sourceVertex = edge.sourceVertex();
        Vertex destinationVertex = edge.destinationVertex();
        sourceAndDestination.put(
                sourceVertex.uri(),
                sourceVertex
        );
        sourceAndDestination.put(
                destinationVertex.uri(),
                destinationVertex
        );
        setPrivateAndPublicSearchContextToFriendlyResource(
                sourceAndDestination,
                sourceAndDestination,
                edge
        );
    }

    @Override
    public void indexSchema(SchemaPojo schema) {
        setPrivateAndPublicSearchContextToFriendlyResource(
                schema.getProperties(),
                schema.getProperties(),
                schema
        );
    }

    @Override
    public void indexProperty(GraphElementPojo property, SchemaPojo schema) {
        Map<URI, SchemaPojo> context = new HashMap<>();
        context.put(
                schema.uri(),
                schema
        );
        setPrivateAndPublicSearchContextToFriendlyResource(
                context,
                context,
                property
        );
    }

    @Override
    public void deleteGraphElement(GraphElement graphElement) {

    }

    @Override
    public void commit() {

    }

    private void setPrivateAndPublicSearchContextToFriendlyResource(Map<URI, ? extends GraphElement> privateContext, Map<URI, ? extends GraphElement> publicContext, FriendlyResource friendlyResource) {
        Neo4jFriendlyResource neo4jFriendlyResource = neo4jFriendlyResourceFactory.withUri(
                friendlyResource.uri()
        );
        NoExRun.wrap(() -> {
            String query = String.format(
                    "%s SET n.private_context='%s', " +
                            "n.public_context='%s'",
                    neo4jFriendlyResource.queryPrefix(),
                    convertGraphElementsToContextJsonString(privateContext),
                    convertGraphElementsToContextJsonString(publicContext)
            );
            return connection.createStatement().execute(
                    query
            );
        }).get();
    }

    private Map<URI, String> mapOfGraphElementsToMapOfLabels(Map<URI, ? extends GraphElement> mapOfGraphElements) {
        Map<URI, String> mapOfLabels = new HashMap<>();
        for (GraphElement graphElement : mapOfGraphElements.values()) {
            mapOfLabels.put(
                    graphElement.uri(),
                    graphElement.label()
            );
        }
        return mapOfLabels;
    }

    private String mapOfUrisAndLabelToJsonString(Map<URI, String> mapOfGraphElements) {
        return JsonUtils.getGson().toJson(
                mapOfGraphElements
        );
    }

    private String convertGraphElementsToContextJsonString(Map<URI, ? extends GraphElement> mapOfGraphElements) {
        return mapOfUrisAndLabelToJsonString(
                mapOfGraphElementsToMapOfLabels(
                        mapOfGraphElements
                )
        );
    }
}
