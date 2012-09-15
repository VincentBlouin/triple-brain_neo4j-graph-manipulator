package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.ReadableIndex;
import org.triple_brain.module.common_utils.Uris;
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

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JVertex extends Vertex {

    protected User owner;
    protected Node node;
    protected Neo4JGraphElement graphElement;
    private ReadableIndex<Node> nodeIndex;

    protected Neo4JVertexFactory vertexFactory;

    protected Neo4JEdgeFactory edgeFactory;

    protected SuggestionNeo4JConverter suggestionConverter;

    protected FriendlyResourceNeo4JUtils friendlyResourceUtils;

    @AssistedInject
    protected Neo4JVertex(
            ReadableIndex<Node> nodeIndex,
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            SuggestionNeo4JConverter suggestionConverter,
            FriendlyResourceNeo4JUtils friendlyResourceUtils,
            @Assisted Node node,
            @Assisted User owner
    ) {
        this.nodeIndex = nodeIndex;
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.suggestionConverter = suggestionConverter;
        this.friendlyResourceUtils = friendlyResourceUtils;
        this.node = node;
        this.owner = owner;
        graphElement = Neo4JGraphElement.withPropertyContainer(node);
    }

    @AssistedInject
    protected Neo4JVertex(
            ReadableIndex<Node> nodeIndex,
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            SuggestionNeo4JConverter suggestionConverter,
            FriendlyResourceNeo4JUtils friendlyResourceUtils,
            @Assisted Node node,
            @Assisted URI uri,
            @Assisted User owner
    ) {
        this(nodeIndex, vertexFactory, edgeFactory, suggestionConverter, friendlyResourceUtils, node, owner);
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
                owner
        );
    }

    @Override
    public Edge addRelationToVertex(Vertex destinationVertex) {
        Node destinationNode = nodeIndex.get(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                destinationVertex.id()
        ).getSingle();

        Relationship relationship = node.createRelationshipTo(
                destinationNode,
                Relationships.TRIPLE_BRAIN_EDGE
        );
        return edgeFactory.createWithRelationshipAndOwner(
                relationship,
                owner
        );
    }

    @Override
    public void remove() {
        for (Edge edge : connectedEdges()) {
            edge.remove();
        }
        removePropertiesWithRelationShipType(Relationships.SUGGESTION);
        removePropertiesWithRelationShipType(Relationships.TYPE);
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
        removePropertiesWithRelationShipType(Relationships.SUGGESTION);
        for(Suggestion suggestion : suggestions){
            Node suggestionAsNode = suggestionConverter.createSuggestion(suggestion);
            node.createRelationshipTo(suggestionAsNode, Relationships.SUGGESTION);
        }
    }

    private void removePropertiesWithRelationShipType(RelationshipType relationshipType){
        for (Relationship relationship : node.getRelationships(Direction.OUTGOING, relationshipType)) {
            Node toDelete = relationship.getEndNode();
            Neo4JUtils.removeAllProperties(toDelete);
            Neo4JUtils.removeAllProperties(relationship);
            Neo4JUtils.removeOutgoingNodesRecursively(toDelete);
            relationship.delete();
        }
    }

    @Override
    public Set<Suggestion> suggestions() {
        Set<Suggestion> suggestions = new HashSet<>();
        for (Relationship relationship : node.getRelationships(Relationships.SUGGESTION)) {
            Suggestion suggestion = suggestionConverter.nodeToSuggestion(
                    relationship.getEndNode()
            );
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    @Override
    public FriendlyResource friendlyResourceWithUri(URI uri) {
        return friendlyResourceUtils.loadFromUri(uri);
    }

    @Override
    public void addType(FriendlyResource type) {
        Node typeAsNode = friendlyResourceUtils.addInGraph(type);
        node.createRelationshipTo(
                typeAsNode,
                Relationships.TYPE
        );
    }

    @Override
    public void removeFriendlyResource(FriendlyResource friendlyResource) {
        friendlyResourceUtils.remove(friendlyResource);
    }

    @Override
    public Set<FriendlyResource> getAdditionalTypes() {
        Set<FriendlyResource> additionalTypes = new HashSet<FriendlyResource>();
        for (Relationship relationship : node.getRelationships(Relationships.TYPE)) {
            FriendlyResource type = friendlyResourceUtils.loadFromNode(
                    relationship.getEndNode()
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
