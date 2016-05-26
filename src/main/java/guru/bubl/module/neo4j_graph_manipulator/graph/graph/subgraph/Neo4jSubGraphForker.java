/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.subgraph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.subgraph.SubGraph;
import guru.bubl.module.model.graph.subgraph.SubGraphForker;
import guru.bubl.module.model.graph.vertex.*;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;

import java.net.URI;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;

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
            forkVertexIfApplicableUsingCache(
                    vertexOperator,
                    subGraph.vertexWithIdentifier(
                            vertex.uri()
                    )
            );
            return;
        }
        Collection<EdgePojo> edges = (Collection<EdgePojo>) subGraph.edges().values();
        for (EdgePojo edgePojo : edges) {
            EdgeOperator edgeOperator = edgeFactory.withUriAndSourceAndDestinationVertex(
                    edgePojo.uri(),
                    edgePojo.sourceVertex(),
                    edgePojo.destinationVertex()
            );
            VertexOperator sourceVertexOriginal = vertexFactory.withUri(
                    edgePojo.sourceVertex().uri()
            );
            VertexOperator destinationVertexOriginal = vertexFactory.withUri(
                    edgeOperator.destinationVertex().uri()
            );
            forkVertexIfApplicableUsingCache(
                    sourceVertexOriginal,
                    subGraph.vertexWithIdentifier(
                            sourceVertexOriginal.uri()
                    )
            );
            forkVertexIfApplicableUsingCache(
                    destinationVertexOriginal,
                    subGraph.vertexWithIdentifier(
                            destinationVertexOriginal.uri()
                    )
            );
            if (hasForkedVertex(sourceVertexOriginal) && hasForkedVertex(destinationVertexOriginal)) {
                edgeOperator.forkUsingSourceAndDestinationVertexAndCache(
                        getForkedVertex(sourceVertexOriginal),
                        getForkedVertex(destinationVertexOriginal),
                        edgePojo
                );
            }
        }
    }

    private Boolean hasForkedVertex(Vertex vertex) {
        return forkedVertices.containsKey(
                vertex.uri()
        );
    }

    private void forkVertexIfApplicableUsingCache(VertexOperator originalVertex, VertexInSubGraph vertexCache) {
        if (hasForkedVertex(originalVertex) || !originalVertex.isPublic()) {
            return;
        }
        forkedVertices.put(
                originalVertex.uri(),
                originalVertex.forkForUserUsingCache(
                        user,
                        vertexCache
                )
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
