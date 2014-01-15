package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.FriendlyResourceOperator;
import org.triple_brain.module.model.graph.edge.Edge;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.vertex.Vertex;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraph;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphOperator;
import org.triple_brain.module.model.suggestion.Suggestion;
import org.triple_brain.module.model.suggestion.SuggestionOperator;

import javax.inject.Inject;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JVertexInSubGraphOperator implements VertexInSubGraphOperator {

    public static final String NUMBER_OF_CONNECTED_EDGES_PROPERTY_NAME = "number_of_connected_edges_property_name";

    private Integer depthInSubGraph = -1;
    protected Node node;
    protected Neo4JGraphElementOperator graphElementOperator;
    protected Neo4JVertexFactory vertexFactory;

    protected FriendlyResourceOperator friendlyResource;

    protected Neo4JEdgeFactory edgeFactory;

    protected Neo4JUtils utils;

    protected Neo4JSuggestionFactory suggestionFactory;

    protected Neo4JGraphElementFactory neo4JGraphElementFactory;

    @Inject
    Neo4JFriendlyResourceFactory friendlyResourceFactory;

    @AssistedInject
    protected Neo4JVertexInSubGraphOperator(
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            Neo4JUtils utils,
            Neo4JGraphElementFactory neo4JGraphElementFactory,
            Neo4JFriendlyResourceFactory neo4JFriendlyResourceFactory,
            Neo4JSuggestionFactory suggestionFactory,
            @Assisted Node node
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.suggestionFactory = suggestionFactory;
        this.utils = utils;
        this.friendlyResourceFactory = neo4JFriendlyResourceFactory;
        this.node = node;
        this.neo4JGraphElementFactory = neo4JGraphElementFactory;
        graphElementOperator = neo4JGraphElementFactory.withNode(
                node
        );
        this.friendlyResource = neo4JFriendlyResourceFactory.createOrLoadFromNode(
                node
        );
        if (!node.hasProperty(IS_PUBLIC_PROPERTY_NAME)) {
            makePrivate();
        }
        if (!isInitialized()) {
            initialize();
        }
    }

    @AssistedInject
    protected Neo4JVertexInSubGraphOperator(
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            Neo4JUtils utils,
            Neo4JGraphElementFactory neo4JGraphElementFactory,
            Neo4JFriendlyResourceFactory friendlyResourceFactory,
            Neo4JSuggestionFactory suggestionFactory,
            @Assisted URI uri
    ) {
        this(
                vertexFactory,
                edgeFactory,
                utils,
                neo4JGraphElementFactory,
                friendlyResourceFactory,
                suggestionFactory,
                utils.getOrCreate(uri)
        );
    }

    @AssistedInject
    protected Neo4JVertexInSubGraphOperator(
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            Neo4JUtils utils,
            Neo4JGraphElementFactory neo4JGraphElementFactory,
            Neo4JFriendlyResourceFactory friendlyResourceFactory,
            Neo4JSuggestionFactory suggestionFactory,
            @Assisted String ownerUserName
    ) {
        this(
                vertexFactory,
                edgeFactory,
                utils,
                neo4JGraphElementFactory,
                friendlyResourceFactory,
                suggestionFactory,
                new UserUris(ownerUserName).generateVertexUri()
        );
    }

    @AssistedInject
    protected Neo4JVertexInSubGraphOperator(
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            Neo4JUtils utils,
            Neo4JGraphElementFactory neo4JGraphElementFactory,
            Neo4JFriendlyResourceFactory friendlyResourceFactory,
            Neo4JSuggestionFactory suggestionFactory,
            @Assisted Set<Vertex> includedVertices,
            @Assisted Set<Edge> includedEdges
    ) throws IllegalArgumentException {
        this(
                vertexFactory,
                edgeFactory,
                utils,
                neo4JGraphElementFactory,
                friendlyResourceFactory,
                suggestionFactory,
                new UserUris(
                        includedVertices.iterator().next().ownerUsername()
                ).generateVertexUri()
        );
        if (includedVertices.size() <= 1) {
            throw new IllegalArgumentException(
                    "A minimum number of 2 vertices is required to create a vertex from included vertices"
            );
        }
        setIncludedVertices(
                includedVertices
        );
        setIncludedEdges(
                includedEdges
        );
    }

    @Override
    public boolean hasEdge(Edge edge) {
        for (Relationship relationship : relationshipsToEdges()) {
            Edge edgeToCompare = edgeFactory.createOrLoadWithNode(
                    relationship.getStartNode()
            );
            if (edge.equals(edgeToCompare)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public EdgeOperator edgeThatLinksToDestinationVertex(Vertex destinationVertex) {
        for (Relationship relationship : relationshipsToEdges()) {
            EdgeOperator edge = edgeFactory.createOrLoadWithNode(
                    relationship.getStartNode()
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
        Iterable<Relationship> relationshipsIt = node.getRelationships(
                Relationships.SOURCE_VERTEX
        );
        for (Relationship relationship : relationshipsIt) {
            Edge edge = edgeFactory.createOrLoadWithNode(
                    relationship.getStartNode()
            );
            if (edge.destinationVertex().equals(destinationVertex)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public EdgeOperator addVertexAndRelation() {
        Neo4JVertexInSubGraphOperator newVertexOperator = vertexFactory.createForOwnerUsername(
                ownerUsername()
        );
        return addRelationToVertex(newVertexOperator);
    }

    @Override
    public EdgeOperator addRelationToVertex(Vertex destinationVertex) {
        incrementNumberOfConnectedEdges();
        ((Neo4JVertexInSubGraphOperator) destinationVertex).incrementNumberOfConnectedEdges();
        return edgeFactory.createForSourceAndDestinationVertex(
                this,
                (Neo4JVertexInSubGraphOperator) destinationVertex
        );
    }

    @Override
    public void remove() {
        for (Edge edge : connectedEdges()) {
            EdgeOperator edgeOperator = (EdgeOperator) edge;
            edgeOperator.remove();
        }
        graphElementOperator.remove();
    }

    @Override
    public String ownerUsername() {
        return graphElementOperator.ownerUsername();
    }

    @Override
    public Set<EdgeOperator> connectedEdges() {
        Set<EdgeOperator> edges = new HashSet<EdgeOperator>();
        for (Relationship relationship : relationshipsToEdges()) {
            edges.add(
                    edgeFactory.createOrLoadWithNode(
                            relationship.getStartNode()
                    )
            );
        }
        return edges;
    }

    @Override
    public Integer getNumberOfConnectedEdges() {
        return Integer.valueOf(node.getProperty(
                NUMBER_OF_CONNECTED_EDGES_PROPERTY_NAME
        ).toString());
    }

    @Override
    public void setNumberOfConnectedEdges(Integer numberOfConnectedEdges) {
        node.setProperty(
                NUMBER_OF_CONNECTED_EDGES_PROPERTY_NAME,
                numberOfConnectedEdges
        );
    }

    protected void incrementNumberOfConnectedEdges() {
        node.setProperty(
                NUMBER_OF_CONNECTED_EDGES_PROPERTY_NAME,
                getNumberOfConnectedEdges() + 1
        );
    }

    protected void decrementNumberOfConnectedEdges() {
        node.setProperty(
                NUMBER_OF_CONNECTED_EDGES_PROPERTY_NAME,
                getNumberOfConnectedEdges() - 1
        );
    }

    private Iterable<Relationship> relationshipsToEdges() {
        return node.getRelationships(
                Relationships.SOURCE_VERTEX,
                Relationships.DESTINATION_VERTEX
        );
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
        graphElementOperator.updateLastModificationDate();
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
        graphElementOperator.addType(type);
    }

    @Override
    public void removeIdentification(FriendlyResource friendlyResource) {
        graphElementOperator.removeIdentification(
                friendlyResource
        );
        removeSuggestionsHavingExternalResourceAsOrigin(
                friendlyResource
        );
    }


    private void removeSuggestionsHavingExternalResourceAsOrigin(FriendlyResource resource) {
        for (Suggestion suggestion : suggestions()) {
            SuggestionOperator suggestionOperator = (SuggestionOperator) suggestion;
            suggestionOperator.removeOriginsThatDependOnResource(
                    resource
            );
            if (suggestion.origins().isEmpty()) {
                suggestionOperator.remove();
            }
        }
    }

    @Override
    public Set<FriendlyResource> getAdditionalTypes() {
        return graphElementOperator.getAdditionalTypes();
    }

    @Override
    public Set<FriendlyResource> getIdentifications() {
        return graphElementOperator.getIdentifications();
    }

    @Override
    public void addSameAs(FriendlyResource friendlyResourceImpl) {
        graphElementOperator.addSameAs(friendlyResourceImpl);
    }

    @Override
    public Set<FriendlyResource> getSameAs() {
        return graphElementOperator.getSameAs();
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
        graphElementOperator.updateLastModificationDate();
    }

    @Override
    public void makePrivate() {
        node.setProperty(
                IS_PUBLIC_PROPERTY_NAME,
                false
        );
        graphElementOperator.updateLastModificationDate();
    }

    @Override
    public Set<Vertex> getIncludedVertices() {
        Set<Vertex> includedGraphElements = new HashSet<>();
        for (Relationship relationship : node.getRelationships(Relationships.HAS_INCLUDED_VERTEX, Direction.OUTGOING)) {
            includedGraphElements.add(
                    vertexFactory.createOrLoadUsingNode(
                            relationship.getEndNode()
                    )
            );
        }
        return includedGraphElements;
    }

    @Override
    public Set<Edge> getIncludedEdges() {
        Set<Edge> includedGraphElements = new HashSet<>();
        for (Relationship relationship : node.getRelationships(Relationships.HAS_INCLUDED_EDGE, Direction.OUTGOING)) {
            includedGraphElements.add(
                    edgeFactory.createOrLoadWithNode(
                            relationship.getEndNode()
                    )
            );
        }
        return includedGraphElements;
    }

    public void setIncludedVertices(Set<Vertex> includedVertices) {
        for (Vertex vertex: includedVertices) {
            node.createRelationshipTo(
                    utils.getFromUri(vertex.uri()),
                    Relationships.HAS_INCLUDED_VERTEX
            );
        }
    }

    public void setIncludedEdges(Set<Edge> includedEdges) {
        for (Edge edge: includedEdges) {
            node.createRelationshipTo(
                    utils.getFromUri(edge.uri()),
                    Relationships.HAS_INCLUDED_EDGE
            );
        }
    }

    @Override
    public DateTime creationDate() {
        return graphElementOperator.creationDate();
    }

    @Override
    public DateTime lastModificationDate() {
        return graphElementOperator.lastModificationDate();
    }

    @Override
    public URI uri() {
        return graphElementOperator.uri();
    }

    @Override
    public String label() {
        return graphElementOperator.label();
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
    public String comment() {
        return friendlyResource.comment();
    }

    @Override
    public void comment(String comment) {
        friendlyResource.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return friendlyResource.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        friendlyResource.addImages(
                images
        );
    }

    @Override
    public void label(String label) {
        graphElementOperator.label(label);
    }

    @Override
    public boolean hasLabel() {
        return graphElementOperator.hasLabel();
    }

    @Override
    public void addGenericIdentification(FriendlyResource friendlyResource) {
        graphElementOperator.addGenericIdentification(friendlyResource);
    }

    @Override
    public Set<FriendlyResource> getGenericIdentifications() {
        return graphElementOperator.getGenericIdentifications();
    }

    @Override
    public boolean equals(Object vertexToCompareAsObject) {
        return graphElementOperator.equals(vertexToCompareAsObject);
    }

    @Override
    public int hashCode() {
        return graphElementOperator.hashCode();
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

    private void addVertexType() {
        node.addLabel(
                Neo4JAppLabels.vertex
        );
    }

    private boolean isInitialized() {
        return hasVertexType();
    }

    private void initialize() {
        addVertexType();
        initNumberOfConnectedEdges();
    }

    private void initNumberOfConnectedEdges() {
        node.setProperty(
                NUMBER_OF_CONNECTED_EDGES_PROPERTY_NAME,
                0
        );
    }

    private boolean hasVertexType() {
        return node.hasLabel(
                Neo4JAppLabels.vertex
        );
    }
}
