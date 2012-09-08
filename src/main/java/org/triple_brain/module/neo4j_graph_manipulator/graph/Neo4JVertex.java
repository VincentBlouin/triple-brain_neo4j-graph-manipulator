package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Suggestion;
import org.triple_brain.module.model.TripleBrainUris;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.Vertex;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.hp.hpl.jena.vocabulary.RDF.type;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JVertex extends Vertex {

    protected User owner;
    protected Node node;
    protected Neo4JGraphElement graphElement;

    public static Neo4JVertex loadUsingNodeOfOwner(Node node, User owner) {
        return new Neo4JVertex(
                node,
                owner
        );
    }

    public static Neo4JVertex createUsingEmptyNodeUriAndOwner(Node node, URI uri, User owner) {
        Neo4JVertex vertex = new Neo4JVertex(node, owner);
        vertex.graphElement = Neo4JGraphElement.initiateProperties(node, uri);
        node.setProperty(type.getURI(), TripleBrainUris.TRIPLE_BRAIN_VERTEX);
        return vertex;
    }

    protected Neo4JVertex(Node node, User owner) {
        this.node = node;
        this.owner = owner;
        graphElement = Neo4JGraphElement.withPropertyContainer(node);
    }

    @Override
    public boolean hasEdge(Edge edge) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addOutgoingEdge(Edge edge) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeOutgoingEdge(Edge edge) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Edge edgeThatLinksToDestinationVertex(Vertex destinationVertex) {
        for (Relationship relationship : connectedEdgesAsRelationships()) {
            Edge edge = Neo4JEdge.loadWithRelationshipOfOwner(
                    relationship,
                    owner
            );
            if (edge.hasVertex(destinationVertex)) {
                return edge;
            }
        }
        throw new RuntimeException(
                "Edge between vertex with " + id() +
                        " and vertex with id " + destinationVertex.id() +
                        " was not found"
        );
    }

    @Override
    public boolean hasDestinationVertex(Vertex destinationVertex) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addNeighbor(Vertex neighbor) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Edge addVertexAndRelation() {
        Node newVertexNode = node.getGraphDatabase().createNode();
        Neo4JVertex.createUsingEmptyNodeUriAndOwner(
                newVertexNode,
                owner.generateUri(),
                owner
        );
        Relationship newRelationship = node.createRelationshipTo(
                newVertexNode,
                Relationships.TRIPLE_BRAIN_EDGE
        );
        return Neo4JEdge.createWithRelationshipAndOwner(
                newRelationship,
                owner
        );
    }

    @Override
    public Edge addRelationToVertex(Vertex destinationVertex) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void remove() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeNeighbor(Vertex neighbor) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Edge> outGoingEdges() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Edge> connectedEdges() {
        Set<Edge> edges = new HashSet<Edge>();
        for (Relationship relationship : connectedEdgesAsRelationships()) {
            edges.add(
                    Neo4JEdge.loadWithRelationshipOfOwner(
                            relationship,
                            owner
                    )
            );
        }
        return edges;
    }

    private Iterable<Relationship> connectedEdgesAsRelationships() {
        return node.getRelationships(Relationships.TRIPLE_BRAIN_EDGE);
    }

    @Override
    public List<String> hiddenConnectedEdgesLabel() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void hiddenConnectedEdgesLabel(List<String> hiddenEdgeLabel) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasMinNumberOfEdgesFromCenterVertex() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void suggestions(Set<Suggestion> suggestions) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Suggestion> suggestions() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FriendlyResource friendlyResourceWithUri(URI uri) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addType(FriendlyResource type) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeFriendlyResource(FriendlyResource type) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<FriendlyResource> getAdditionalTypes() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addSameAs(FriendlyResource friendlyResource) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<FriendlyResource> getSameAs() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
