package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.TripleBrainUris;
import org.triple_brain.module.model.graph.SubGraph;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.vertex.Vertex;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraph;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphOperator;
import scala.collection.immutable.Map;

import javax.inject.Inject;
import java.util.HashSet;

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
            new HashSet<VertexInSubGraphOperator>(),
            new HashSet<EdgeOperator>()
    );

    @Inject
    Neo4JUtils neo4JUtils;

    @Inject
    Neo4JFriendlyResourceFactory neo4JFriendlyResourceFactory;

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
                Neo4JVertexInSubGraphOperator vertexOperator = vertexFactory.createOrLoadUsingNode(
                        node
                );
                Integer distanceFromCenterVertex = (Integer) (
                        row.get("length(path)").get()
                );
                subGraph.vertices().add(
                        vertexOperator
                );
                setDistanceFromCenterVertexToVertexIfApplicable(
                        vertexOperator,
                        distanceFromCenterVertex / 2
                );
            }else{
                subGraph.edges().add(
                        edgeFactory.createOrLoadWithNode(
                                node
                        )
                );
            }
        }
        return subGraph;
    }

    private Boolean isNodeVertex(Node node){
        if(!node.hasRelationship(Relationships.TYPE)){
            return false;
        }
        for(Relationship relationship : node.getRelationships(Relationships.TYPE)){
            FriendlyResource type = neo4JFriendlyResourceFactory.createOrLoadFromNode(
                    relationship.getEndNode()
            );
            if(TripleBrainUris.TRIPLE_BRAIN_VERTEX.equals(type.uri().toString())){
                return true;
            }
        }
        return false;
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
                "MATCH path=start_node<-[:" +
                    Relationships.SOURCE_VERTEX+
                "|" + Relationships.DESTINATION_VERTEX + "*0.." + depth * 2 +
                "]->in_path_node " +
                "RETURN start_node,in_path_node, length(path)";
    }
}
