package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.graph.*;
import org.triple_brain.module.model.graph.edge.Edge;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.edge.EdgePojo;
import org.triple_brain.module.model.graph.vertex.Vertex;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraph;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphPojo;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphOperator;
import scala.collection.immutable.Map;

import javax.inject.Inject;
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
    private SubGraph subGraph = SubGraphImpl.withVerticesAndEdges(
            new HashSet<VertexInSubGraph>(),
            new HashSet<Edge>()
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

    public SubGraph load() {
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
        Edge edge = new EdgePojo(
                graphElementFromOperator(edgeOperator),
                edgeOperator.sourceVertex(),
                edgeOperator.destinationVertex()
        );
        subGraph.edges().add(edge);
        return edge;
    }

    private VertexInSubGraph addVertexIfNotAddedYetUsingOperator(VertexInSubGraphOperator vertexOperator){
        if(subGraph.vertices().contains(vertexOperator)){
            return subGraph.vertexWithIdentifier(vertexOperator.uri());
        }
        VertexInSubGraph vertex = new VertexInSubGraphPojo(
                graphElementFromOperator(vertexOperator),
                vertexOperator.getNumberOfConnectedEdges(),
                vertexOperator.getIncludedVertices(),
                vertexOperator.getIncludedEdges(),
                vertexOperator.suggestions(),
                vertexOperator.isPublic()
        );
        subGraph.vertices().add(vertex);
        return vertex;
    }

    private GraphElement graphElementFromOperator(GraphElementOperator graphElementOperator){
        return new GraphElementPojo(
                friendlyResourceFromOperator(graphElementOperator),
                graphElementOperator.getGenericIdentifications(),
                graphElementOperator.getSameAs(),
                graphElementOperator.getAdditionalTypes()
        );
    }

    private FriendlyResource friendlyResourceFromOperator(FriendlyResourceOperator friendlyResourceOperator){
        return new FriendlyResourcePojo(
                friendlyResourceOperator.uri(),
                friendlyResourceOperator.label(),
                friendlyResourceOperator.images(),
                friendlyResourceOperator.comment(),
                friendlyResourceOperator.creationDate(),
                friendlyResourceOperator.lastModificationDate()
        );
    }
}
