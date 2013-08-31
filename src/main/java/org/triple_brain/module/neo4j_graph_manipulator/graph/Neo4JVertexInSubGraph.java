package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.ReadableIndex;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.*;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.Vertex;
import org.triple_brain.module.model.graph.VertexInSubGraph;
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

    protected FriendlyResource friendlyResource;

    protected Neo4JEdgeFactory edgeFactory;

    protected Neo4JUtils utils;
    protected List<String> hiddenEdgesLabel = new ArrayList<String>();

    private static final String HIDDEN_EDGES_LABEL_KEY = "hidden_edges_label";

    protected Neo4JSuggestionFactory suggestionFactory;

    @AssistedInject
    protected Neo4JVertexInSubGraph(
            ReadableIndex<Node> nodeIndex,
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            Neo4JUtils utils,
            Neo4JGraphElementFactory neo4JGraphElementFactory,
            Neo4JFriendlyResourceFactory neo4JFriendlyResourceFactory,
            Neo4JSuggestionFactory suggestionFactory,
            @Assisted Node node,
            @Assisted User owner
    ) {
        this.nodeIndex = nodeIndex;
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.suggestionFactory = suggestionFactory;
        this.utils = utils;
        this.node = node;
        graphElement = neo4JGraphElementFactory.withPropertyContainerAndOwner(
                node,
                owner
        );
        this.friendlyResource = neo4JFriendlyResourceFactory.createOrLoadFromNode(
                node
        );
    }

    @AssistedInject
    protected Neo4JVertexInSubGraph(
            ReadableIndex<Node> nodeIndex,
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            Neo4JUtils utils,
            Neo4JGraphElementFactory neo4JGraphElementFactory,
            Neo4JFriendlyResourceFactory friendlyResourceFactory,
            Neo4JSuggestionFactory suggestionFactory,
            @Assisted Node node,
            @Assisted URI uri,
            @Assisted User owner
    ) {
        this(
                nodeIndex,
                vertexFactory,
                edgeFactory,
                utils,
                neo4JGraphElementFactory,
                friendlyResourceFactory,
                suggestionFactory,
                node,
                owner
        );
        this.graphElement = neo4JGraphElementFactory.initiatePropertiesAndSetOwner(
                node,
                uri,
                owner
        );
        makePrivate();
        this.addType(friendlyResourceFactory.createOrLoadUsingUriAndLabel(
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
                "Edge between vertex with " + uri() +
                        " and vertex with uri " + destinationVertex.uri() +
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
                destinationVertex.uri()
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
            Neo4JSuggestion neo4JSuggestion = (Neo4JSuggestion) suggestion;
            node.createRelationshipTo(
                    neo4JSuggestion.getNode(),
                    Relationships.SUGGESTION
            );
        }
        graphElement.updateLastModificationDate();
    }

    private void removePropertiesWithRelationShipType(RelationshipType relationshipType) {
        for (Relationship relationship : node.getRelationships(Direction.OUTGOING, relationshipType)) {
            utils.removeAllProperties(relationship);
            relationship.delete();
        }
    }

    @Override
    public Set<Suggestion> suggestions() {
        Set<Suggestion> suggestions = new HashSet<>();
        for (Relationship relationship : node.getRelationships(Relationships.SUGGESTION)) {
            Suggestion suggestion = suggestionFactory.getFromNode(
                    relationship.getEndNode()
            );
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    @Override
    public void addType(FriendlyResource type) {
        graphElement.addType(type);
    }

    @Override
    public void removeFriendlyResource(FriendlyResource friendlyResource) {
        graphElement.removeFriendlyResource(
                friendlyResource
        );
        removeSuggestionsHavingExternalResourceAsOrigin(
                friendlyResource
        );
    }


    private void removeSuggestionsHavingExternalResourceAsOrigin(FriendlyResource resource) {
        for (Suggestion suggestion : suggestions()) {
            suggestion.removeOriginsThatDependOnResource(
                    resource
            );
            if (suggestion.origins().isEmpty()) {
                suggestion.remove();
            }
        }
    }

    @Override
    public Set<FriendlyResource> getAdditionalTypes() {
        return graphElement.getAdditionalTypes();
    }

    @Override
    public void addSameAs(FriendlyResource friendlyResourceImpl) {
        graphElement.addSameAs(friendlyResourceImpl);
    }

    @Override
    public Set<FriendlyResource> getSameAs() {
        return graphElement.getSameAs();
    }

    private final String IS_PUBLIC_PROPERTY_NAME = "is_public";

    @Override
    public Boolean isPublic() {
        return (Boolean) node.getProperty(
                IS_PUBLIC_PROPERTY_NAME
        );
    }

    @Override
    public void makePublic() {
        node.setProperty(
                IS_PUBLIC_PROPERTY_NAME,
                true
        );
        graphElement.updateLastModificationDate();
    }

    @Override
    public void makePrivate() {
        node.setProperty(
                IS_PUBLIC_PROPERTY_NAME,
                false
        );
        graphElement.updateLastModificationDate();
    }

    @Override
    public DateTime creationDate() {
        return graphElement.creationDate();
    }

    @Override
    public DateTime lastModificationDate() {
        return graphElement.lastModificationDate();
    }

    @Override
    public URI uri() {
        return graphElement.uri();
    }

    @Override
    public String note() {
        if (!node.hasProperty(RDFS.comment.getURI())) {
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
        graphElement.updateLastModificationDate();
    }

    @Override
    public String label() {
        return graphElement.label();
    }

    @Override
    public Set<Image> images() {
        return friendlyResource.images();
    }

    @Override
    public Boolean gotTheImages() {
        return friendlyResource.gotTheImages();
    }

    @Override
    public String description() {
        return friendlyResource.description();
    }

    @Override
    public void description(String description) {
        friendlyResource.description(
                description
        );
    }

    @Override
    public Boolean gotADescription() {
        return friendlyResource.gotADescription();
    }

    @Override
    public void addImages(Set<Image> images) {
        friendlyResource.addImages(
                images
        );
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
        return uri().equals(vertexToCompare.uri());
    }

    @Override
    public int hashCode() {
        return uri().hashCode();
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
