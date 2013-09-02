package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.TripleBrainUris;
import org.triple_brain.module.model.graph.*;
import scala.collection.immutable.Map;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Iterator;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JSubGraphExtractor {
    Neo4JVertexFactory vertexFactory;
    Neo4JEdgeFactory edgeFactory;
    ExecutionEngine engine;
    Vertex centerVertex;
    Integer depth;
    private SubGraph subGraph = Neo4JSubGraph.withVerticesAndEdges(
            new HashSet<VertexInSubGraph>(),
            new HashSet<Edge>()
    );

    @Inject
    Neo4JUtils neo4JUtils;

    @AssistedInject
    protected Neo4JSubGraphExtractor(
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            ExecutionEngine engine,
            @Assisted Vertex centerVertex,
            @Assisted Integer depth
    ) {
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
            Node node = (Node) row.get("in_path_node").get();
            if(isNodeVertex(node)){
                Neo4JVertexInSubGraph vertex = vertexFactory.loadUsingNodeOfOwner(
                        node,
                        centerVertex.owner()
                );
                Integer distanceFromCenterVertex = (Integer) (
                        row.get("length(path)").get()
                );
                subGraph.vertices().add(
                        vertex
                );
                setDistanceFromCenterVertexToVertexIfApplicable(
                        vertex,
                        distanceFromCenterVertex / 2
                );
                subGraph.edges().addAll(vertex.connectedEdges());
            }

        }
        removeEdgesThatDontHaveAllTheirVerticesInSubGraph();
        return subGraph;
    }

    private Boolean isNodeVertex(Node node){
        if(!node.hasProperty(Relationships.TYPE.name())){
            return false;
        }
        String typesListAsString = (String) node.getProperty(
                Relationships.TYPE.name()
        );
        return typesListAsString.contains(TripleBrainUris.TRIPLE_BRAIN_VERTEX);
    }

    private void setDistanceFromCenterVertexToVertexIfApplicable(Vertex vertex, Integer distance){
        VertexInSubGraph vertexInSubGraph = subGraph.vertexWithIdentifier(vertex.uri());
        if(vertexInSubGraph.minDistanceFromCenterVertex() == -1 || vertexInSubGraph.minDistanceFromCenterVertex() > distance){
            vertexInSubGraph.setMinDistanceFromCenterVertex(distance);
        }
    }

    private String queryToGetGraph() {
        Node centerVertexAsNode = neo4JUtils.nodeOfVertex(centerVertex);
        return "START start_node=node(" + centerVertexAsNode.getId() + ")" +
                "MATCH path=start_node<-[" +
                ":" + Relationships.SOURCE_VERTEX+
                "|:" + Relationships.DESTINATION_VERTEX + "*0.." + depth * 2 +
                "]->in_path_node " +
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
                frontierVertex = subGraph.vertexWithIdentifier(frontierVertex.uri());
                frontierVertex.hiddenConnectedEdgesLabel().add(
                        edge.label()
                );
                iterator.remove();
            }
        }
    }
}
