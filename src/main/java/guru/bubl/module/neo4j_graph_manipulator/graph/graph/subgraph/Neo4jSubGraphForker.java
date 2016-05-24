/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.subgraph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.subgraph.SubGraph;
import guru.bubl.module.model.graph.subgraph.SubGraphForker;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexFactory;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;

import java.net.URI;
import java.sql.Connection;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Neo4jSubGraphForker implements SubGraphForker {

    Neo4jEdgeFactory edgeFactory;
    VertexFactory vertexFactory;
    Connection connection;
    User user;

    private HashMap<URI, VertexOperator> forkedVertices;

    @AssistedInject
    protected Neo4jSubGraphForker(
            Neo4jEdgeFactory edgeFactory,
            Neo4jVertexFactory vertexFactory,
            Connection connection,
            @Assisted User user
    ) {
        this.user = user;
        this.edgeFactory = edgeFactory;
        this.vertexFactory = vertexFactory;
        this.connection = connection;
    }

    @Override
    public void fork(SubGraph subGraph) {
        forkedVertices = new HashMap<>();
        if (subGraph.edges().isEmpty()) {
            Vertex vertex = subGraph.vertices().values().iterator().next();
            VertexOperator vertexOperator = vertexFactory.withUri(
                    vertex.uri()
            );
            forkVertexIfApplicable(vertexOperator);
            return;
        }
        for (Edge edge : subGraph.edges().values()) {
            EdgeOperator edgeOperator = edgeFactory.withUriAndSourceAndDestinationVertex(
                    edge.uri(),
                    edge.sourceVertex(),
                    edge.destinationVertex()
            );
            VertexOperator sourceVertexOriginal = edgeOperator.sourceVertex();
            VertexOperator destinationVertexOriginal = edgeOperator.destinationVertex();
            forkVertexIfApplicable(
                    sourceVertexOriginal
            );
            forkVertexIfApplicable(
                    destinationVertexOriginal
            );
            if (hasForkedVertex(sourceVertexOriginal) && hasForkedVertex(destinationVertexOriginal)) {
                edgeOperator.forkUsingSourceAndDestinationVertex(
                        getForkedVertex(sourceVertexOriginal),
                        getForkedVertex(destinationVertexOriginal)
                );
            }
        }
    }

    private Boolean hasForkedVertex(Vertex vertex) {
        return forkedVertices.containsKey(
                vertex.uri()
        );
    }

    private void forkVertexIfApplicable(VertexOperator originalVertex) {
        if (hasForkedVertex(originalVertex) || !originalVertex.isPublic()) {
            return;
        }
        forkedVertices.put(
                originalVertex.uri(),
                originalVertex.forkForUser(user)
        );
    }

    private VertexOperator getForkedVertex(Vertex originalVertex) {
        return forkedVertices.get(
                originalVertex.uri()
        );
    }

//    this.subGraph = subGraph;
//    String query = String.format(
//            "START edge=node:node_auto_index('" +
//                    Neo4jFriendlyResource.props.uri + ":[%s]) " +
//                    "MATCH edge<-[:" +
//                    Relationships.SOURCE_VERTEX +
//                    "|" + Relationships.DESTINATION_VERTEX + ,
//            "]->vertex";
//    buildEdgesUriStr()
//    );
//    NoExRun.wrap(() ->
//            connection.createStatement().executeQuery(
//            query
//            )).get();

//    private String buildEdgesUriStr() {
//        return subGraph.edges().values().stream()
//                .map(i -> i.uri().toString())
//                .collect(Collectors.joining(","));
//
//    }

}
