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
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexFactory;
import guru.bubl.module.model.graph.vertex.VertexInSubGraph;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;
import org.neo4j.driver.v1.Session;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SubGraphForkerNeo4j implements SubGraphForker {

    EdgeFactoryNeo4j edgeFactory;
    VertexFactory vertexFactory;
    Session session;
    User user;

    private HashMap<URI, VertexOperator> forkedVertices;

    @AssistedInject
    protected SubGraphForkerNeo4j(
            EdgeFactoryNeo4j edgeFactory,
            VertexFactoryNeo4j vertexFactory,
            Session session,
            @Assisted User user
    ) {
        this.user = user;
        this.edgeFactory = edgeFactory;
        this.vertexFactory = vertexFactory;
        this.session = session;
    }

    @Override
    public Map<URI, VertexOperator> fork(SubGraph subGraph) {
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
            return forkedVertices;
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
        return forkedVertices;
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

}
