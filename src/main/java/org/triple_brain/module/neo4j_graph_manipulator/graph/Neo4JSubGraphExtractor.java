package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.ReadableIndex;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.SubGraph;
import org.triple_brain.module.model.graph.Vertex;
import scala.collection.immutable.Map;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Iterator;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JSubGraphExtractor {

    private GraphDatabaseService graphDb;
    private ReadableIndex<Node> nodeIndex;
    Neo4JVertexFactory vertexFactory;
    Neo4JEdgeFactory edgeFactory;
    ExecutionEngine engine;
    Vertex centerVertex;
    Integer depth;
    private SubGraph subGraph = Neo4JSubGraph.withVerticesAndEdges(
            new HashSet<Vertex>(),
            new HashSet<Edge>()
    );

    @Inject
    Neo4JUtils neo4JUtils;

    @AssistedInject
    protected Neo4JSubGraphExtractor(
            GraphDatabaseService graphDb,
            ReadableIndex<Node> nodeIndex,
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            ExecutionEngine engine,
            @Assisted Vertex centerVertex,
            @Assisted Integer depth
    ) {
        this.graphDb = graphDb;
        this.nodeIndex = nodeIndex;
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.engine = engine;
        this.centerVertex = centerVertex;
        this.depth = depth;
    }

    public SubGraph load() {
        ExecutionResult result = engine.execute(
                queryToGetGraph()
        );
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            Neo4JVertex vertex = vertexFactory.loadUsingNodeOfOwner(
                    (Node) row.get("in_path_node").get(),
                    centerVertex.owner()
            );
            int distanceFromCenterVertex = (Integer) (
                    row.get("length(path)").get()
            );
            subGraph.vertices().add(
                    vertex
            );
            subGraph.edges().addAll(vertex.connectedEdges());
        }
        removeEdgesThatDontHaveAllTheirVerticesInSubGraph();
        return subGraph;
    }

    private String queryToGetGraph() {
        Node centerVertexAsNode = neo4JUtils.nodeOfVertex(centerVertex);
        return "START start_node=node(" + centerVertexAsNode.getId() + ")" +
                "MATCH path=start_node<-[:" + Relationships.TRIPLE_BRAIN_EDGE + "*0.." + depth + "]->in_path_node " +
                "RETURN start_node,in_path_node, length(path)";
    }

    private void removeEdgesThatDontHaveAllTheirVerticesInSubGraph() {
        Iterator<Edge> iterator = subGraph.edges().iterator();
        while (iterator.hasNext()) {
            Edge edge = iterator.next();
            Vertex sourceVertex = edge.sourceVertex();

            Vertex destinationVertex = edge.destinationVertex();

            boolean shouldRemoveEdge =
                    !subGraph.vertices().contains(sourceVertex) ||
                            !subGraph.vertices().contains(destinationVertex);
            if (shouldRemoveEdge) {
                Vertex frontierVertex = subGraph.vertices().contains(sourceVertex) ?
                        sourceVertex :
                        destinationVertex;
                frontierVertex = subGraph.vertexWithIdentifier(frontierVertex.id());
                frontierVertex.hiddenConnectedEdgesLabel().add(
                        edge.label()
                );
                iterator.remove();
            }
        }
    }
}
