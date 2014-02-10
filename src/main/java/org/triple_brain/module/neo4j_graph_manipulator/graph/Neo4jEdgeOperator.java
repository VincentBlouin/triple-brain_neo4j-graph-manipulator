package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.vertex.Vertex;
import org.triple_brain.module.model.graph.vertex.VertexOperator;

import java.net.URI;
import java.util.Date;
import java.util.Set;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jEdgeOperator implements EdgeOperator {

    protected Node node;
    protected Neo4jGraphElementOperator graphElementOperator;
    protected Neo4jVertexFactory vertexFactory;
    protected Neo4jEdgeFactory edgeFactory;

    @AssistedInject
    protected Neo4jEdgeOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory neo4jGraphElementFactory,
            @Assisted Node node
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.node = node;
        graphElementOperator = neo4jGraphElementFactory.withNode(node);
    }

    @AssistedInject
    protected Neo4jEdgeOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory neo4jGraphElementFactory,
            Neo4jUtils neo4jUtils,
            @Assisted URI uri
    ) {
        this(
                vertexFactory,
                edgeFactory,
                neo4jGraphElementFactory,
                neo4jUtils.getOrCreate(uri)
        );
    }

    @AssistedInject
    protected Neo4jEdgeOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory neo4jGraphElementFactory,
            Neo4jUtils neo4jUtils,
            @Assisted("source") Neo4jVertexInSubGraphOperator sourceVertexOperator,
            @Assisted("destination") Neo4jVertexInSubGraphOperator destinationVertexOperator
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        UserUris userUris = new UserUris(
                sourceVertexOperator.ownerUsername()
        );
        Node newEdgeNode = neo4jUtils.create(
                userUris.generateEdgeUri()
        );
        newEdgeNode.createRelationshipTo(
                sourceVertexOperator.node,
                Relationships.SOURCE_VERTEX
        );
        newEdgeNode.createRelationshipTo(
                destinationVertexOperator.node,
                Relationships.DESTINATION_VERTEX
        );
        this.node = newEdgeNode;
        this.graphElementOperator = neo4jGraphElementFactory.withNode(
                node
        );
    }

    @Override
    public VertexOperator sourceVertex() {
        return vertexFactory.createOrLoadUsingNode(
                relationshipWithSourceVertex().getEndNode()
        );
    }

    private VertexOperator sourceVertexOperator(){
        return vertexFactory.createOrLoadUsingNode(
                relationshipWithSourceVertex().getEndNode()
        );
    }

    @Override
    public VertexOperator destinationVertex() {
        return vertexFactory.createOrLoadUsingNode(
                relationshipWithDestinationVertex().getEndNode()
        );
    }

    private VertexOperator destinationVertexOperator(){
        return vertexFactory.createOrLoadUsingNode(
                relationshipWithDestinationVertex().getEndNode()
        );
    }

    @Override
    public VertexOperator otherVertex(Vertex vertex) {
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
    public void inverse() {
        Relationship destinationRelation = relationshipWithDestinationVertex();
        Node destinationNode = destinationRelation.getEndNode();
        Relationship sourceRelation = relationshipWithSourceVertex();
        Node sourceNode = sourceRelation.getEndNode();
        destinationRelation.delete();
        sourceRelation.delete();
        node.createRelationshipTo(
                destinationNode,
                Relationships.SOURCE_VERTEX
        );
        node.createRelationshipTo(
                sourceNode,
                Relationships.DESTINATION_VERTEX
        );
    }

    @Override
    public void remove() {
        VertexOperator sourceVertex = sourceVertexOperator();
        sourceVertex.setNumberOfConnectedEdges(
                sourceVertex.getNumberOfConnectedEdges() - 1
        );
        VertexOperator destinationVertex = destinationVertexOperator();
        destinationVertex.setNumberOfConnectedEdges(
                destinationVertex.getNumberOfConnectedEdges() - 1
        );
        graphElementOperator.remove();
    }

    @Override
    public String ownerUsername() {
        return graphElementOperator.ownerUsername();
    }

    @Override
    public Date creationDate() {
        return graphElementOperator.creationDate();
    }

    @Override
    public Date lastModificationDate() {
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
    public void label(String label) {
        graphElementOperator.label(label);
    }

    @Override
    public Set<Image> images() {
        return graphElementOperator.images();
    }

    @Override
    public Boolean gotImages() {
        return graphElementOperator.gotImages();
    }

    @Override
    public String comment() {
        return graphElementOperator.comment();
    }

    @Override
    public void comment(String comment) {
        graphElementOperator.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return graphElementOperator.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        graphElementOperator.addImages(images);
    }

    @Override
    public boolean hasLabel() {
        return graphElementOperator.hasLabel();
    }

    @Override
    public void addGenericIdentification(FriendlyResource friendlyResource) {
        graphElementOperator.addGenericIdentification(
                friendlyResource
        );
    }

    @Override
    public Set<FriendlyResource> getGenericIdentifications() {
        return graphElementOperator.getGenericIdentifications();
    }

    @Override
    public void addSameAs(FriendlyResource friendlyResourceImpl) {
        graphElementOperator.addSameAs(friendlyResourceImpl);
    }

    @Override
    public Set<FriendlyResource> getSameAs() {
        return graphElementOperator.getSameAs();
    }

    @Override
    public void addType(FriendlyResource type) {
        graphElementOperator.addType(type);
    }

    @Override
    public void removeIdentification(FriendlyResource type) {
        graphElementOperator.removeIdentification(type);
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
    public boolean equals(Object edgeToCompareAsObject) {
        return graphElementOperator.equals(edgeToCompareAsObject);
    }

    @Override
    public int hashCode() {
        return graphElementOperator.hashCode();
    }

    private Relationship relationshipWithSourceVertex() {
        return node.getRelationships(
                Relationships.SOURCE_VERTEX
        ).iterator().next();
    }

    private Relationship relationshipWithDestinationVertex() {
        return node.getRelationships(
                Relationships.DESTINATION_VERTEX
        ).iterator().next();
    }
}
