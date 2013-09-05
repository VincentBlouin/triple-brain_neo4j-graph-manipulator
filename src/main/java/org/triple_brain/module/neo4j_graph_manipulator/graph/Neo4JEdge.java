package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.joda.time.DateTime;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.Vertex;

import java.net.URI;
import java.util.Set;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JEdge implements Edge{

    protected Node node;
    protected Neo4JGraphElement graphElement;
    protected Neo4JVertexFactory vertexFactory;
    protected Neo4JEdgeFactory edgeFactory;

    @AssistedInject
    protected Neo4JEdge(
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            Neo4JGraphElementFactory neo4JGraphElementFactory,
            @Assisted Node node,
            @Assisted User owner
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.node = node;
        graphElement = node.hasProperty(Neo4JUserGraph.URI_PROPERTY_NAME) ?
                neo4JGraphElementFactory.withNodeAndOwner(node, owner) :
                neo4JGraphElementFactory.initiatePropertiesAndSetOwner(
                        node,
                        new UserUris(owner).generateEdgeUri(),
                        owner
                );
    }

    @AssistedInject
    protected Neo4JEdge(
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            Neo4JGraphElementFactory neo4JGraphElementFactory,
            Neo4JUtils neo4JUtils,
            @Assisted URI uri,
            @Assisted User owner
    ){
        this(
                vertexFactory,
                edgeFactory,
                neo4JGraphElementFactory,
                neo4JUtils.getOrCreate(uri),
                owner
        );
    }

    @AssistedInject
    protected Neo4JEdge(
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            Neo4JGraphElementFactory neo4JGraphElementFactory,
            GraphDatabaseService graphDb,
            @Assisted("source") Neo4JVertexInSubGraph sourceVertex,
            @Assisted("destination") Neo4JVertexInSubGraph destinationVertex
    ){
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        User owner = sourceVertex.owner();
        if(sourceVertex.hasDestinationVertex(destinationVertex)){
            for(Edge edge : sourceVertex.connectedEdges()){
                if(edge.destinationVertex().equals(destinationVertex)){
                    Neo4JEdge neo4JEdge = (Neo4JEdge) edge;
                    this.node = neo4JEdge.node;
                }
            }
            this.graphElement = neo4JGraphElementFactory.withNodeAndOwner(
                    node,
                    owner
            );
        }else{
            Node newEdgeNode = graphDb.createNode();
            newEdgeNode.createRelationshipTo(
                    sourceVertex.node,
                    Relationships.SOURCE_VERTEX
            );
            newEdgeNode.createRelationshipTo(
                    destinationVertex.node,
                    Relationships.DESTINATION_VERTEX
            );
            this.node = newEdgeNode;
            this.graphElement = neo4JGraphElementFactory.initiatePropertiesAndSetOwner(
                    node,
                    new UserUris(owner).generateEdgeUri(),
                    owner
            );
        }
    }

    @Override
    public Vertex sourceVertex() {
        return vertexFactory.loadUsingNodeOfOwner(
                relationshipWithSourceVertex().getEndNode(),
                graphElement.owner()
        );
    }

    @Override
    public Vertex destinationVertex() {
        return vertexFactory.loadUsingNodeOfOwner(
                relationshipWithDestinationVertex().getEndNode(),
                graphElement.owner()
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
        graphElement.remove();
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
    public String label() {
        return graphElement.label();
    }

    @Override
    public void label(String label) {
        graphElement.label(label);
    }

    @Override
    public Set<Image> images() {
        return graphElement.images();
    }

    @Override
    public Boolean gotTheImages() {
        return graphElement.gotTheImages();
    }

    @Override
    public String comment() {
        return graphElement.comment();
    }

    @Override
    public void comment(String comment) {
        graphElement.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return graphElement.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        graphElement.addImages(images);
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
    public void addSameAs(FriendlyResource friendlyResourceImpl) {
        graphElement.addSameAs(friendlyResourceImpl);
    }

    @Override
    public Set<FriendlyResource> getSameAs() {
        return graphElement.getSameAs();
    }

    @Override
    public void addType(FriendlyResource type) {
        graphElement.addType(type);
    }

    @Override
    public void removeIdentification(FriendlyResource type) {
        graphElement.removeIdentification(type);
    }

    @Override
    public Set<FriendlyResource> getAdditionalTypes() {
        return graphElement.getAdditionalTypes();
    }

    @Override
    public Set<FriendlyResource> getIdentifications() {
        return graphElement.getIdentifications();
    }

    @Override
    public boolean equals(Object edgeToCompareAsObject) {
        Edge edgeToCompare = (Edge) edgeToCompareAsObject;
        return uri().equals(edgeToCompare.uri());
    }

    @Override
    public int hashCode() {
        return uri().hashCode();
    }

    private Relationship relationshipWithSourceVertex(){
        return node.getRelationships(
                Relationships.SOURCE_VERTEX
        ).iterator().next();
    }

    private Relationship relationshipWithDestinationVertex(){
        return node.getRelationships(
                Relationships.DESTINATION_VERTEX
        ).iterator().next();
    }
}
