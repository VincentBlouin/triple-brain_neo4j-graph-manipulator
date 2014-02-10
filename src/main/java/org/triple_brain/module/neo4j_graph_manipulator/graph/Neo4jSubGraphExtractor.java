package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.graph.SubGraphPojo;
import org.triple_brain.module.model.graph.edge.Edge;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.edge.EdgePojo;
import org.triple_brain.module.model.graph.vertex.Vertex;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraph;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphOperator;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphPojo;
import scala.collection.immutable.Map;

import javax.inject.Inject;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jSubGraphExtractor {
    Neo4jVertexFactory vertexFactory;
    Neo4jEdgeFactory edgeFactory;
    ExecutionEngine engine;
    Vertex centerVertex;
    Integer depth;
    private SubGraphPojo subGraph = SubGraphPojo.withVerticesAndEdges(
            new HashMap<URI, VertexInSubGraphPojo>(),
            new HashSet<EdgePojo>()
    );

    @Inject
    Neo4jUtils neo4jUtils;

    @Inject
    Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory;

    @AssistedInject
    protected Neo4jSubGraphExtractor(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
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

    public SubGraphPojo load() {
        ExecutionResult result = engine.execute(
                queryToGetGraph()
        );
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            Node node = (Node) row.get("in_path_node").get();
            if(isNodeVertex(node)){
                Neo4jVertexInSubGraphOperator vertexOperator = vertexFactory.createOrLoadUsingNode(
                        node
                );
                VertexInSubGraph vertexInSubGraph = addVertexIfNotAddedYetUsingOperator(
                        vertexOperator
                );
                Integer distanceFromCenterVertex = (Integer) (
                        row.get("length(path)").get()
                );
                setDistanceFromCenterVertexToVertexIfApplicable(
                        vertexInSubGraph,
                        distanceFromCenterVertex / 2
                );
            }else{
                addEdgeIfNotAddedYetUsingOperator(
                        edgeFactory.createOrLoadWithNode(
                                node
                        )
                );
            }
        }
        return subGraph;
    }

    private Boolean isNodeVertex(Node node){
        return node.hasLabel(Neo4jAppLabels.vertex);
    }

    private void setDistanceFromCenterVertexToVertexIfApplicable(
            VertexInSubGraph vertexInSubGraph,
            Integer distance
    ){
        if(vertexInSubGraph.minDistanceFromCenterVertex() == -1 || vertexInSubGraph.minDistanceFromCenterVertex() > distance){
            vertexInSubGraph.setMinDistanceFromCenterVertex(distance);
        }
    }

    private String queryToGetGraph() {
        Node centerVertexAsNode = neo4jUtils.nodeOfVertex(centerVertex);
        return "START start_node=node(" + centerVertexAsNode.getId() + ")" +
                "MATCH path=start_node<-[:" +
                    Relationships.SOURCE_VERTEX+
                "|" + Relationships.DESTINATION_VERTEX + "*0.." + depth * 2 +
                "]->in_path_node " +
                "RETURN start_node,in_path_node, length(path)";
    }

    private Edge addEdgeIfNotAddedYetUsingOperator(EdgeOperator edgeOperator){
        if(subGraph.edges().contains(edgeOperator)){
            return subGraph.edgeWithIdentifier(edgeOperator.uri());
        }
        EdgePojo edge = new EdgePojo(
                edgeOperator
        );
        subGraph.addEdge(edge);
        return edge;
    }

    private VertexInSubGraph addVertexIfNotAddedYetUsingOperator(VertexInSubGraphOperator vertexOperator){
        if(subGraph.vertices().contains(vertexOperator)){
            return subGraph.vertexWithIdentifier(vertexOperator.uri());
        }
        VertexInSubGraphPojo vertex = new VertexInSubGraphPojo(
                vertexOperator
        );
        subGraph.addVertex(vertex);
        return vertex;
    }
}
