package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.ReadableIndex;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Suggestion;
import org.triple_brain.module.model.TripleBrainUris;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.Vertex;

import javax.inject.Inject;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JVertex extends Vertex {

    protected User owner;
    protected Node node;
    protected Neo4JGraphElement graphElement;
    private ReadableIndex<Node> nodeIndex;

    @Inject
    protected Neo4JVertexFactory vertexFactory;

    @Inject
    protected Neo4JEdgeFactory edgeFactory;

    @Inject
    protected SuggestionNeo4JConverter suggestionNeo4JConverter;

    @AssistedInject
    protected Neo4JVertex(
            ReadableIndex<Node> nodeIndex,
            @Assisted Node node,
            @Assisted User owner
    ) {
        this.nodeIndex = nodeIndex;
        this.node = node;
        this.owner = owner;
        graphElement = Neo4JGraphElement.withPropertyContainer(node);
    }

    @AssistedInject
    protected Neo4JVertex(
            ReadableIndex<Node> nodeIndex,
            @Assisted Node node,
            @Assisted URI uri,
            @Assisted User owner
    ) {
        this(nodeIndex, node, owner);
        this.graphElement = Neo4JGraphElement.initiateProperties(node, uri);
        this.addType(FriendlyResource.withUriAndLabel(
                Uris.get(TripleBrainUris.TRIPLE_BRAIN_VERTEX),
                ""
        ));
    }

    @Override
    public boolean hasEdge(Edge edge) {
        for (Relationship relationship : connectedEdgesAsRelationships()) {
            Edge edgeToCompare = edgeFactory.loadWithRelationshipOfOwner(
                    relationship,
                    owner
            );
            if (edge.equals(edgeToCompare)) {
                return true;
            }
        }
        return false;
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
            Edge edge = edgeFactory.loadWithRelationshipOfOwner(
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
        vertexFactory.createUsingEmptyNodeUriAndOwner(
                newVertexNode,
                owner.generateUri(),
                owner
        );
        Relationship newRelationship = node.createRelationshipTo(
                newVertexNode,
                Relationships.TRIPLE_BRAIN_EDGE
        );
        return edgeFactory.createWithRelationshipAndOwner(
                newRelationship,
                owner,
                owner.generateUri()
        );
    }

    @Override
    public Edge addRelationToVertex(Vertex destinationVertex) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void remove() {
        for (Edge edge : connectedEdges()) {
            edge.remove();
        }
        for (Relationship relationship : node.getRelationships(Direction.OUTGOING, Relationships.TYPE)) {
            relationship.getEndNode().delete();
        }
        for (Relationship relationship : node.getRelationships()) {
            relationship.delete();
        }

        node.removeProperty(Neo4JUserGraph.URI_PROPERTY_NAME);
        node.delete();
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
                    edgeFactory.loadWithRelationshipOfOwner(
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

    }

    @Override
    public Set<Suggestion> suggestions() {
        Set<Suggestion> suggestions = new HashSet<>();
        for (Relationship relationship : node.getRelationships(Relationships.SUGGESTION)) {
            Suggestion suggestion = suggestionNeo4JConverter.nodeToSuggestion(
                    relationship.getEndNode()
            );
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    @Override
    public FriendlyResource friendlyResourceWithUri(URI uri) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addType(FriendlyResource type) {
        Node typeAsNode = node.getGraphDatabase().createNode();
        typeAsNode.setProperty(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                type.uri().toString()
        );
        typeAsNode.setProperty(
                RDFS.label.getURI(),
                type.label()
        );
        node.createRelationshipTo(
                typeAsNode,
                Relationships.TYPE
        );
    }

    @Override
    public void removeFriendlyResource(FriendlyResource friendlyResource) {
        Node node = nodeIndex.get(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                friendlyResource.uri().toString()
        ).getSingle();
        node.getRelationships()
                .iterator()
                .next()
                .delete();
        node.removeProperty(RDFS.label.getURI());
        node.removeProperty(Neo4JUserGraph.URI_PROPERTY_NAME);
        node.delete();
    }

    @Override
    public Set<FriendlyResource> getAdditionalTypes() {
        Set<FriendlyResource> additionalTypes = new HashSet<FriendlyResource>();
        for (Relationship relationship : node.getRelationships(Relationships.TYPE)) {
            Node node = relationship.getEndNode();
            FriendlyResource type = FriendlyResource.withUriAndLabel(
                    Uris.get(node.getProperty(Neo4JUserGraph.URI_PROPERTY_NAME).toString()),
                    node.getProperty(RDFS.label.getURI()).toString()
            );
            if (!type.uri().toString().equals(TripleBrainUris.TRIPLE_BRAIN_VERTEX)) {
                additionalTypes.add(type);
            }
        }
        return additionalTypes;
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
        return graphElement.hasLabel();
    }
}
