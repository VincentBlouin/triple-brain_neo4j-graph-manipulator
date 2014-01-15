package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.triple_brain.module.model.graph.SubGraph;
import org.triple_brain.module.model.graph.edge.Edge;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.vertex.Vertex;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphOperator;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JSubGraph implements SubGraph{

    private Set<VertexInSubGraphOperator> vertices = new HashSet<VertexInSubGraphOperator>();
    private Set<EdgeOperator> edges = new HashSet<EdgeOperator>();

    public static Neo4JSubGraph withVerticesAndEdges(Set<VertexInSubGraphOperator> vertices, Set<EdgeOperator> edges){
        return new Neo4JSubGraph(vertices, edges);
    }

    protected Neo4JSubGraph(Set<VertexInSubGraphOperator> vertices, Set<EdgeOperator> edges){
        this.vertices = vertices;
        this.edges = edges;
    }

    @Override
    public VertexInSubGraphOperator vertexWithIdentifier(URI identifier) {
        for(VertexInSubGraphOperator vertex : vertices()){
            if(vertex.uri().equals(identifier)){
                return vertex;
            }
        }
        throw new RuntimeException("vertex with identifier " + identifier + " not found");
    }

    @Override
    public Edge edgeWithIdentifier(URI identifier) {
        for(Edge edge : edges()){
            if(edge.uri().equals(identifier)){
                return edge;
            }
        }
        throw new RuntimeException("edge with identifier " + identifier + " not found");
    }

    @Override
    public int numberOfEdgesAndVertices() {
        return numberOfEdges() +
                numberOfVertices();
    }

    @Override
    public int numberOfEdges() {
        return edges.size();
    }

    @Override
    public int numberOfVertices() {
        return vertices.size();
    }

    @Override
    public boolean containsVertex(Vertex vertex) {
        return vertices.contains(vertex);
    }

    @Override
    public Set<VertexInSubGraphOperator> vertices() {
        return vertices;
    }

    @Override
    public Set<EdgeOperator> edges() {
        return edges;
    }
}
