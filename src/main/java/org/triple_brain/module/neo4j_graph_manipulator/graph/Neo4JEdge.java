package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.model.ExternalFriendlyResource;
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

    private Relationship relationship;
    protected Neo4JGraphElement graphElement;
    protected Neo4JVertexFactory vertexFactory;
    protected Neo4JEdgeFactory edgeFactory;

    @AssistedInject
    protected Neo4JEdge(
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            Neo4JGraphElementFactory neo4JGraphElementFactory,
            @Assisted Relationship relationship,
            @Assisted User owner
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.relationship = relationship;
        graphElement = relationship.hasProperty(Neo4JUserGraph.URI_PROPERTY_NAME) ?
                neo4JGraphElementFactory.withPropertyContainerAndOwner(relationship, owner) :
                neo4JGraphElementFactory.initiatePropertiesAndSetOwner(
                        relationship,
                        new UserUris(owner).generateEdgeUri(),
                        owner
                );
    }

    @Override
    public Vertex sourceVertex() {
        return vertexFactory.loadUsingNodeOfOwner(
                relationship.getStartNode(),
                graphElement.owner()
        );
    }

    @Override
    public Vertex destinationVertex() {
        return vertexFactory.loadUsingNodeOfOwner(
                relationship.getEndNode(),
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
        relationship.removeProperty(Neo4JUserGraph.URI_PROPERTY_NAME);
        relationship.delete();
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

    @Override
    public User owner() {
        return graphElement.owner();
    }

    @Override
    public void addSameAs(ExternalFriendlyResource friendlyResource) {
        graphElement.addSameAs(friendlyResource);
    }

    @Override
    public Set<ExternalFriendlyResource> getSameAs() {
        return graphElement.getSameAs();
    }

    @Override
    public ExternalFriendlyResource friendlyResourceWithUri(URI uri) {
        return graphElement.friendlyResourceWithUri(uri);
    }

    @Override
    public void addType(ExternalFriendlyResource type) {
        graphElement.addType(type);
    }

    @Override
    public void removeFriendlyResource(ExternalFriendlyResource type) {
        graphElement.removeFriendlyResource(type);
    }

    @Override
    public Set<ExternalFriendlyResource> getAdditionalTypes() {
        return graphElement.getAdditionalTypes();
    }

    @Override
    public boolean equals(Object edgeToCompareAsObject) {
        Edge edgeToCompare = (Edge) edgeToCompareAsObject;
        return id().equals(edgeToCompare.id());
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }
}
