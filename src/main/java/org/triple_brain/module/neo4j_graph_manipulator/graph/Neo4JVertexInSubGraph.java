package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.ReadableIndex;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.ExternalFriendlyResource;
import org.triple_brain.module.model.TripleBrainUris;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.Vertex;
import org.triple_brain.module.model.graph.VertexInSubGraph;
import org.triple_brain.module.model.suggestion.PersistedSuggestion;
import org.triple_brain.module.model.suggestion.Suggestion;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JVertexInSubGraph implements VertexInSubGraph {

    private Integer depthInSubGraph = -1;
    protected Node node;
    protected Neo4JGraphElement graphElement;
    private ReadableIndex<Node> nodeIndex;

    protected Neo4JVertexFactory vertexFactory;

    protected Neo4JEdgeFactory edgeFactory;

    protected SuggestionNeo4JConverter suggestionConverter;

    protected Neo4JExternalFriendlyResourcePersistenceUtils friendlyResourceUtils;

    protected Neo4JUtils utils;
    protected List<String> hiddenEdgesLabel = new ArrayList<String>();

    private static final String HIDDEN_EDGES_LABEL_KEY = "hidden_edges_label";

    protected Neo4JExternalResourceUtils externalResourceUtils;

    @AssistedInject
    protected Neo4JVertexInSubGraph(
            ReadableIndex<Node> nodeIndex,
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            SuggestionNeo4JConverter suggestionConverter,
            Neo4JExternalFriendlyResourcePersistenceUtils friendlyResourceUtils,
            Neo4JUtils utils,
            Neo4JExternalResourceUtils externalResourceUtils,
            @Assisted Node node,
            @Assisted User owner
    ) {
        this.nodeIndex = nodeIndex;
        this.externalResourceUtils = externalResourceUtils;
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.suggestionConverter = suggestionConverter;
        this.friendlyResourceUtils = friendlyResourceUtils;
        this.utils = utils;
        this.node = node;
        graphElement = Neo4JGraphElement.withPropertyContainerAndOwner(node, owner);
    }

    @AssistedInject
    protected Neo4JVertexInSubGraph(
            ReadableIndex<Node> nodeIndex,
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            SuggestionNeo4JConverter suggestionConverter,
            Neo4JExternalFriendlyResourcePersistenceUtils friendlyResourceUtils,
            Neo4JUtils utils,
            Neo4JExternalResourceUtils externalResourceUtils,
            @Assisted Node node,
            @Assisted URI uri,
            @Assisted User owner
    ) {
        this(nodeIndex, vertexFactory, edgeFactory, suggestionConverter, friendlyResourceUtils, utils, externalResourceUtils, node, owner);
        this.graphElement = Neo4JGraphElement.initiatePropertiesAndSetOwner(node, uri, owner);
        this.addType(ExternalFriendlyResource.withUriAndLabel(
                Uris.get(TripleBrainUris.TRIPLE_BRAIN_VERTEX),
                ""
        ));
    }

    @Override
    public boolean hasEdge(Edge edge) {
        for (Relationship relationship : connectedEdgesAsRelationships()) {
            Edge edgeToCompare = edgeFactory.loadWithRelationshipOfOwner(
                    relationship,
                    graphElement.owner()
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
                    graphElement.owner()
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
    public Boolean hasDestinationVertex(Vertex destinationVertex) {
        for (Relationship relationship : node.getRelationships(Relationships.TRIPLE_BRAIN_EDGE, Direction.OUTGOING)) {
            Vertex endVertex = vertexFactory.loadUsingNodeOfOwner(
                    relationship.getEndNode(),
                    owner()
            );
            if (destinationVertex.equals(endVertex)) {
                return true;
            }
        }
        return false;
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
                new UserUris(owner()).generateVertexUri(),
                graphElement.owner()
        );
        Relationship newRelationship = node.createRelationshipTo(
                newVertexNode,
                Relationships.TRIPLE_BRAIN_EDGE
        );
        return edgeFactory.createWithRelationshipAndOwner(
                newRelationship,
                graphElement.owner()
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
                graphElement.owner()
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
                            graphElement.owner()
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
        return hiddenEdgesLabel;
    }

    @Override
    public void hiddenConnectedEdgesLabel(List<String> hiddenEdgesLabel) {
        this.hiddenEdgesLabel = hiddenEdgesLabel;
    }

    @Override
    public void addSuggestions(Suggestion... suggestions) {
        for (Suggestion suggestion : suggestions) {
            Node suggestionAsNode = suggestionConverter.createSuggestion(suggestion);
            node.createRelationshipTo(suggestionAsNode, Relationships.SUGGESTION);
        }
    }

    private void removePropertiesWithRelationShipType(RelationshipType relationshipType) {
        for (Relationship relationship : node.getRelationships(Direction.OUTGOING, relationshipType)) {
            utils.removeAllProperties(relationship);
            relationship.delete();
        }
    }

    @Override
    public Set<PersistedSuggestion> suggestions() {
        Set<PersistedSuggestion> suggestions = new HashSet<>();
        for (Relationship relationship : node.getRelationships(Relationships.SUGGESTION)) {
            PersistedSuggestion suggestion = suggestionConverter.nodeToSuggestion(
                    relationship.getEndNode()
            );
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    @Override
    public ExternalFriendlyResource friendlyResourceWithUri(URI uri) {
        return friendlyResourceUtils.getFromUri(uri);
    }

    @Override
    public void addType(ExternalFriendlyResource type) {
        Node typeAsNode = friendlyResourceUtils
                .getOrCreate(type);
        node.createRelationshipTo(
                typeAsNode,
                Relationships.TYPE
        );
    }

    @Override
    public void removeFriendlyResource(ExternalFriendlyResource friendlyResource) {
        removeSuggestionsHavingExternalResourceAsOrigin(friendlyResource);
        Node friendlyResourceAsNode = externalResourceUtils.getFromUri(
                friendlyResource.uri()
        );
        for (Relationship relationship : node.getRelationships(Direction.OUTGOING)) {
            Node endNode = relationship.getEndNode();
            if (endNode.equals(friendlyResourceAsNode)) {
                relationship.delete();
            }
        }
    }

    private void removeSuggestionsHavingExternalResourceAsOrigin(ExternalFriendlyResource externalResource) {
        for (PersistedSuggestion suggestion : suggestions()) {
            suggestion.get().removeOriginsThatDependOnResource(
                    externalResource
            );
            if (suggestion.get().origins().isEmpty()) {
                suggestionConverter.remove(
                        suggestion
                );
            }
        }
    }

    @Override
    public Set<ExternalFriendlyResource> getAdditionalTypes() {
        Set<ExternalFriendlyResource> additionalTypes = new HashSet<ExternalFriendlyResource>();
        for (Relationship relationship : node.getRelationships(Relationships.TYPE)) {
            ExternalFriendlyResource type = friendlyResourceUtils.loadFromNode(
                    relationship.getEndNode()
            );
            if (!type.uri().toString().equals(TripleBrainUris.TRIPLE_BRAIN_VERTEX)) {
                additionalTypes.add(type);
            }
        }
        return additionalTypes;
    }

    @Override
    public void addSameAs(ExternalFriendlyResource friendlyResource) {
        Node sameAsAsNode = friendlyResourceUtils
                .getOrCreate(friendlyResource);

        node.createRelationshipTo(
                sameAsAsNode,
                Relationships.SAME_AS
        );
    }

    @Override
    public Set<ExternalFriendlyResource> getSameAs() {
        Set<ExternalFriendlyResource> sameAsSet = new HashSet<ExternalFriendlyResource>();
        for (Relationship relationship : node.getRelationships(Relationships.SAME_AS)) {
            ExternalFriendlyResource sameAs = friendlyResourceUtils.loadFromNode(
                    relationship.getEndNode()
            );
            sameAsSet.add(sameAs);
        }
        return sameAsSet;
    }

    @Override
    public String id() {
        return graphElement.id();
    }

    @Override
    public String note() {
        if(!node.hasProperty(RDFS.comment.getURI())){
            return "";
        }
        return (String) node.getProperty(
                RDFS.comment.getURI()
        );
    }

    @Override
    public void note(String note) {
        node.setProperty(
                RDFS.comment.getURI(),
                note
        );
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

    @Override
    public User owner() {
        return graphElement.owner();
    }

    @Override
    public boolean equals(Object vertexToCompareAsObject) {
        Vertex vertexToCompare = (Vertex) vertexToCompareAsObject;
        return id().equals(vertexToCompare.id());
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }

    @Override
    public Integer minDistanceFromCenterVertex() {
        return depthInSubGraph;
    }

    @Override
    public VertexInSubGraph setMinDistanceFromCenterVertex(Integer minDistanceFromCenterVertex) {
        this.depthInSubGraph = minDistanceFromCenterVertex;
        return this;
    }

}
