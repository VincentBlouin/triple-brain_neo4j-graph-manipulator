package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.ReadableIndex;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.SubGraph;
import org.triple_brain.module.model.graph.UserGraph;
import org.triple_brain.module.model.graph.Vertex;
import org.triple_brain.module.model.graph.exceptions.InvalidDepthOfSubVerticesException;
import org.triple_brain.module.model.graph.exceptions.NonExistingResourceException;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JUserGraph implements UserGraph {

    public static final String URI_PROPERTY_NAME = "uri";

    private User user;

    private GraphDatabaseService graphDb;
    private ReadableIndex<Node> nodeIndex;
    private ReadableIndex<Relationship> relationshipIndex;
    private Neo4JVertexFactory vertexFactory;
    private Neo4JSubGraphExtractorFactory subGraphExtractorFactory;
    private Neo4JEdgeFactory edgeFactory;


    @AssistedInject
    protected Neo4JUserGraph(
            GraphDatabaseService graphDb,
            ReadableIndex<Node> nodeIndex,
            ReadableIndex<Relationship> relationshipIndex,
            Neo4JVertexFactory vertexFactory,
            Neo4JEdgeFactory edgeFactory,
            Neo4JSubGraphExtractorFactory subGraphExtractorFactory,
            @Assisted User user
    ) {
        this.graphDb = graphDb;
        this.nodeIndex = nodeIndex;
        this.relationshipIndex = relationshipIndex;
        this.user = user;
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.subGraphExtractorFactory = subGraphExtractorFactory;
    }

    @Override
    public Vertex defaultVertex() {
        return vertexWithURI(
                user.defaultVertexUri()
        );
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Boolean haveElementWithId(String id) {
        return nodeIndex.get(
                URI_PROPERTY_NAME,
                id
        ).hasNext() ||
                relationshipIndex.get(
                        URI_PROPERTY_NAME,
                        id
                ).hasNext();
    }

    @Override
    public SubGraph graphWithDepthAndCenterVertexId(Integer depthOfSubVertices, String centerVertexURI) throws NonExistingResourceException {
        if(depthOfSubVertices < 0){
            throw new InvalidDepthOfSubVerticesException(
                    depthOfSubVertices,
                    centerVertexURI
            );
        }
        Node node = nodeIndex.get(
                URI_PROPERTY_NAME,
                centerVertexURI
        ).getSingle();
        if(node == null){
            throw new NonExistingResourceException(
                    centerVertexURI
            );
        }
        Vertex centerVertex = vertexFactory.loadUsingNodeOfOwner(
                node,
                user
        );
        return subGraphExtractorFactory.withCenterVertexAndDepth(
                centerVertex,
                depthOfSubVertices
        ).load();
    }

    @Override
    public SubGraph graphWithDefaultVertexAndDepth(Integer depth) throws InvalidDepthOfSubVerticesException {
        return graphWithDepthAndCenterVertexId(
                depth,
                defaultVertex().id()
        );
    }

    @Override
    public String toRdfXml() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Vertex vertexWithURI(URI uri) {
        Node node = nodeIndex.get(
                URI_PROPERTY_NAME,
                uri.toString()
                ).getSingle();
        return vertexFactory.loadUsingNodeOfOwner(
                node,
                user
        );
    }

    @Override
    public Edge edgeWithUri(URI uri) {
        Relationship relationship = relationshipIndex.get(
                URI_PROPERTY_NAME,
                uri.toString()
        ).getSingle();
        return edgeFactory.loadWithRelationshipOfOwner(
                relationship,
                user
        );
    }
}
