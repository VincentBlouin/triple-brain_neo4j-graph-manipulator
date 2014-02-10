package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.ReadableIndex;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.SubGraphPojo;
import org.triple_brain.module.model.graph.UserGraph;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.exceptions.InvalidDepthOfSubVerticesException;
import org.triple_brain.module.model.graph.exceptions.NonExistingResourceException;
import org.triple_brain.module.model.graph.vertex.Vertex;
import org.triple_brain.module.model.graph.vertex.VertexOperator;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jUserGraph implements UserGraph {

    public static final String URI_PROPERTY_NAME = "uri";

    private User user;
    private ReadableIndex<Node> nodeIndex;
    private ReadableIndex<Relationship> relationshipIndex;
    private Neo4jVertexFactory vertexFactory;
    private Neo4jSubGraphExtractorFactory subGraphExtractorFactory;
    private Neo4jEdgeFactory edgeFactory;


    @AssistedInject
    protected Neo4jUserGraph(
            GraphDatabaseService graphDb,
            ReadableIndex<Node> nodeIndex,
            ReadableIndex<Relationship> relationshipIndex,
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jSubGraphExtractorFactory subGraphExtractorFactory,
            @Assisted User user
    ) {
        this.nodeIndex = nodeIndex;
        this.relationshipIndex = relationshipIndex;
        this.user = user;
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.subGraphExtractorFactory = subGraphExtractorFactory;
    }

    @Override
    public VertexOperator defaultVertex() {
        return vertexWithUri(
                new UserUris(user).defaultVertexUri()
        );
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Boolean haveElementWithId(URI id) {
        return nodeIndex.get(
                URI_PROPERTY_NAME,
                id.toString()
        ).hasNext() ||
                relationshipIndex.get(
                        URI_PROPERTY_NAME,
                        id.toString()
                ).hasNext();
    }

    @Override
    public SubGraphPojo graphWithDepthAndCenterVertexId(Integer depthOfSubVertices, URI centerVertexURI) throws NonExistingResourceException {
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
        Vertex centerVertex = vertexFactory.createOrLoadUsingNode(
                node
        );
        return subGraphExtractorFactory.withCenterVertexAndDepth(
                centerVertex,
                depthOfSubVertices
        ).load();
    }

    @Override
    public SubGraphPojo graphWithDefaultVertexAndDepth(Integer depth) throws InvalidDepthOfSubVerticesException {
        return graphWithDepthAndCenterVertexId(
                depth,
                defaultVertex().uri()
        );
    }

    @Override
    public String toRdfXml() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public VertexOperator vertexWithUri(URI uri) {
        return vertexFactory.createOrLoadUsingUri(
                uri
        );
    }

    @Override
    public EdgeOperator edgeWithUri(URI uri) {
        return edgeFactory.createOrLoadFromUri(
                uri
        );
    }

    @Override
    public VertexOperator createVertex() {
        return vertexFactory.createForOwnerUsername(
                user.username()
        );
    }

}
