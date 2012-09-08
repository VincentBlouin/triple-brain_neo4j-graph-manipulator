package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Suggestion;
import org.triple_brain.module.model.TripleBrainUris;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.Vertex;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static com.hp.hpl.jena.vocabulary.RDF.type;
import static com.hp.hpl.jena.vocabulary.RDFS.label;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JVertex extends Vertex {

    protected User owner;
    protected Node node;

    public static Neo4JVertex loadUsingNodeOfOwner(Node node, User owner){
        return new Neo4JVertex(
                node,
                owner
        );
    }

    public static Neo4JVertex createUsingEmptyNodeUriAndOwner(Node node, String uri, User owner){
        node.setProperty(Neo4JUserGraph.URI_PROPERTY_NAME, uri);
        node.setProperty(label.getURI(), "");
        node.setProperty(type.getURI(), TripleBrainUris.TRIPLE_BRAIN_VERTEX);
        return new Neo4JVertex(node, owner);
    }

    protected Neo4JVertex(Node node, User owner){
        this.node = node;
        this.owner = owner;
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String label() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void label(String label) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasLabel() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
