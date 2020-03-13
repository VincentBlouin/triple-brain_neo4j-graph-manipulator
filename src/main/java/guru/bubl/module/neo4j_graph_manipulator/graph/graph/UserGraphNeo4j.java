/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.exceptions.InvalidDepthOfSubVerticesException;
import guru.bubl.module.model.graph.exceptions.NonExistingResourceException;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.graph.vertex.VertexPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.SubGraphExtractorFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;

import javax.inject.Inject;
import java.net.URI;

import static org.neo4j.driver.v1.Values.parameters;

public class UserGraphNeo4j implements UserGraph {

    public static final String URI_PROPERTY_NAME = "uri";

    private User user;
    private VertexFactoryNeo4j vertexFactory;
    private SubGraphExtractorFactoryNeo4j subGraphExtractorFactory;
    private EdgeFactoryNeo4j edgeFactory;

    @Inject
    Driver driver;

    @AssistedInject
    protected UserGraphNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            SubGraphExtractorFactoryNeo4j subGraphExtractorFactory,
            @Assisted User user
    ) {
        this.user = user;
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.subGraphExtractorFactory = subGraphExtractorFactory;
    }

    @Override
    public VertexOperator defaultVertex() {
        return getAnyVertex();
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Boolean haveElementWithId(URI uri) {
        return FriendlyResourceNeo4j.haveElementWithUri(uri, driver);
    }

    @Override
    public SubGraphPojo graphWithDepthAndCenterBubbleUri(Integer depthOfSubVertices, URI centerBubbleUri) throws NonExistingResourceException {
        return graphWithDepthResultsLimitAndCenterBubbleUri(depthOfSubVertices, null, centerBubbleUri);
    }

    @Override
    public SubGraphPojo aroundVertexUriInShareLevels(URI centerVertexUri, Integer ... shareLevels) throws NonExistingResourceException {
        return subGraphExtractorFactory.withCenterVertexInShareLevels(
                centerVertexUri,
                shareLevels
        ).load();
    }

    @Override
    public SubGraphPojo aroundVertexUriWithDepthInShareLevels(URI centerVertexUri, Integer depth, Integer ... shareLevels) throws NonExistingResourceException {
        return subGraphExtractorFactory.withCenterVertexInShareLevelsAndDepth(
                centerVertexUri,
                depth,
                shareLevels
        ).load();
    }

    @Override
    public SubGraphPojo graphWithDepthResultsLimitAndCenterBubbleUri(Integer depthOfSubVertices, Integer resultsLimit, URI centerBubbleUri) throws NonExistingResourceException {
        if (depthOfSubVertices < 0) {
            throw new InvalidDepthOfSubVerticesException(
                    depthOfSubVertices,
                    centerBubbleUri
            );
        }
        SubGraphPojo subGraph = resultsLimit == null ? subGraphExtractorFactory.withCenterVertexAndDepth(
                centerBubbleUri,
                depthOfSubVertices
        ).load() : subGraphExtractorFactory.withCenterVertexDepthAndResultsLimit(
                centerBubbleUri,
                depthOfSubVertices,
                resultsLimit
        ).load();
        if (subGraph.vertices().isEmpty() && !UserUris.isUriOfAnIdentifier(centerBubbleUri)) {
            throw new NonExistingResourceException(
                    centerBubbleUri
            );
        }
        return subGraph;
    }

    @Override
    public SubGraphPojo graphWithAnyVertexAndDepth(Integer depth) throws InvalidDepthOfSubVerticesException {
        return graphWithDepthAndCenterBubbleUri(
                depth,
                getAnyVertex().uri()
        );
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
        VertexOperator operator = vertexFactory.createForOwner(
                user.username()
        );
        return new VertexPojo(
                operator.uri()
        );
    }

    private VertexOperator getAnyVertex() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    "MATCH(n:Vertex{owner:$owner}) RETURN n.uri limit 1",
                    parameters(
                            "owner", user.username()
                    )
            ).single();
            return vertexFactory.withUri(
                    URI.create(
                            record.get("n.uri").asString()
                    )
            );
        }
    }
}
