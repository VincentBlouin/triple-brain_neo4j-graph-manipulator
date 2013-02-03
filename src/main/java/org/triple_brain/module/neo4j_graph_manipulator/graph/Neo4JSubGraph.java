package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.triple_brain.module.model.graph.*;

import java.util.HashSet;
import java.util.Set;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JSubGraph implements SubGraph{

    private Set<VertexInSubGraph> vertices = new HashSet<VertexInSubGraph>();
    private Set<Edge> edges = new HashSet<Edge>();

    public static Neo4JSubGraph withVerticesAndEdges(Set<VertexInSubGraph> vertices, Set<Edge> edges){
        return new Neo4JSubGraph(vertices, edges);
    }

    protected Neo4JSubGraph(Set<VertexInSubGraph> vertices, Set<Edge> edges){
        this.vertices = vertices;
        this.edges = edges;
    }

    @Override
    public VertexInSubGraph vertexWithIdentifier(String identifier) {
        for(VertexInSubGraph vertex : vertices()){
            if(vertex.id().equals(identifier)){
                return vertex;
            }
        }
        throw new RuntimeException("vertex with identifier " + identifier + " not found");
    }

    @Override
    public Edge edgeWithIdentifier(String identifier) {
        for(Edge edge : edges()){
            if(edge.id().equals(identifier)){
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
    public Set<VertexInSubGraph> vertices() {
        return vertices;
    }

    @Override
    public Set<Edge> edges() {
        return edges;
    }
}
