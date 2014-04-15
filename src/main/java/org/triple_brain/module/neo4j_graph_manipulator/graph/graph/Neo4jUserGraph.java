package org.triple_brain.module.neo4j_graph_manipulator.graph.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.batch.CypherResult;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.SubGraphPojo;
import org.triple_brain.module.model.graph.UserGraph;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.exceptions.InvalidDepthOfSubVerticesException;
import org.triple_brain.module.model.graph.exceptions.NonExistingResourceException;
import org.triple_brain.module.model.graph.vertex.VertexOperator;
import org.triple_brain.module.model.graph.vertex.VertexPojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.Neo4jSubGraphExtractorFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;

import java.net.URI;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

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
    private Neo4jFriendlyResourceFactory friendlyResourceFactory;

    @AssistedInject
    protected Neo4jUserGraph(
            ReadableIndex<Node> nodeIndex,
            ReadableIndex<Relationship> relationshipIndex,
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jSubGraphExtractorFactory subGraphExtractorFactory,
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            @Assisted User user
    ) {
        this.nodeIndex = nodeIndex;
        this.relationshipIndex = relationshipIndex;
        this.user = user;
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.subGraphExtractorFactory = subGraphExtractorFactory;
        this.friendlyResourceFactory = friendlyResourceFactory;
    }

    @Override
    public VertexOperator defaultVertex() {
        return vertexWithUri(
                getDefaultVertexUri()
        );
    }

    private URI getDefaultVertexUri(){
        return new UserUris(
                user
        ).defaultVertexUri();
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
        SubGraphPojo subGraph = subGraphExtractorFactory.withCenterVertexAndDepth(
                centerVertexURI,
                depthOfSubVertices
        ).load();
        if(subGraph.vertices().isEmpty()){
            throw new NonExistingResourceException(
                    centerVertexURI
            );
        }
        return subGraph;
    }

    @Override
    public SubGraphPojo graphWithDefaultVertexAndDepth(Integer depth) throws InvalidDepthOfSubVerticesException {
        return graphWithDepthAndCenterVertexId(
                depth,
                getDefaultVertexUri()
        );
    }

    @Override
    public String toRdfXml() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public VertexOperator vertexWithUri(URI uri) {
        return vertexFactory.withUri(
                uri
        );
    }

    @Override
    public EdgeOperator edgeWithUri(URI uri) {
        return edgeFactory.withUri(
                uri
        );
    }

    @Override
    public VertexPojo createVertex() {
        VertexOperator operator = vertexFactory.createForOwnerUsername(
                user.username()
        );
        return new VertexPojo(
                operator.uri()
        );
    }

}
