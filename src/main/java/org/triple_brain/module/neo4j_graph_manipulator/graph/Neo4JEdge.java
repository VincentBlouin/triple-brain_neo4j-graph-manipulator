package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.Vertex;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JEdge extends Edge {

    private Relationship relationship;
    private User owner;
    protected Neo4JGraphElement graphElement;

    public static Neo4JEdge loadWithRelationshipOfOwner(Relationship relationship, User owner) {
        return new Neo4JEdge(
                relationship,
                owner
        );
    }

    public static Neo4JEdge createWithRelationshipAndOwner(Relationship relationship, User owner) {
        Neo4JEdge edge = new Neo4JEdge(relationship, owner);
        edge.graphElement = Neo4JGraphElement.initiateProperties(
                relationship,
                owner.generateUri()
        );
        return edge;
    }

    protected Neo4JEdge(Relationship relationship, User owner) {
        this.relationship = relationship;
        this.owner = owner;
        graphElement = Neo4JGraphElement.withPropertyContainer(relationship);
    }

    @Override
    public Vertex sourceVertex() {
        return Neo4JVertex.loadUsingNodeOfOwner(
                relationship.getStartNode(),
                owner
        );
    }

    @Override
    public Vertex destinationVertex() {
        return Neo4JVertex.loadUsingNodeOfOwner(
                relationship.getEndNode(),
                owner
        );
    }

    @Override
    public Vertex otherVertex(Vertex vertex) {
        return sourceVertex().equals(vertex) ?
                destinationVertex() :
                sourceVertex();
    }

    @Override
    public boolean hasVertex(Vertex vertex) {
        return sourceVertex().equals(vertex) ||
                destinationVertex().equals(vertex);
    }

    @Override
    public void remove() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String id() {
        return graphElement.id();
    }

    @Override
    public String label() {
        return graphElement.label();
    }

    @Override
    public void label(String label) {
        graphElement.label(label);
    }

    @Override
    public boolean hasLabel() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
