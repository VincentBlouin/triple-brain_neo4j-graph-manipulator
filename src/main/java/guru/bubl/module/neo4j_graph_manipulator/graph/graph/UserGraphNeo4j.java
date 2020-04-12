/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.graph.vertex.VertexPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.SubGraphExtractorFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;
import org.neo4j.driver.v1.Driver;

import javax.inject.Inject;
import java.net.URI;

public class UserGraphNeo4j implements UserGraph {

    public static final String URI_PROPERTY_NAME = "uri";

    private User user;
    private VertexFactoryNeo4j vertexFactory;
    private SubGraphExtractorFactoryNeo4j subGraphExtractorFactory;

    @Inject
    Driver driver;

    @AssistedInject
    protected UserGraphNeo4j(
            VertexFactoryNeo4j vertexFactory,
            SubGraphExtractorFactoryNeo4j subGraphExtractorFactory,
            @Assisted User user
    ) {
        this.user = user;
        this.vertexFactory = vertexFactory;
        this.subGraphExtractorFactory = subGraphExtractorFactory;
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
    public SubGraphPojo aroundVertexUriInShareLevels(URI centerVertexUri, Integer... shareLevels) {
        return subGraphExtractorFactory.withCenterVertexInShareLevels(
                centerVertexUri,
                shareLevels
        ).load();
    }

    @Override
    public SubGraphPojo aroundVertexUriWithDepthInShareLevels(URI centerVertexUri, Integer depth, Integer... shareLevels) {
        return subGraphExtractorFactory.withCenterVertexInShareLevelsAndDepth(
                centerVertexUri,
                depth,
                shareLevels
        ).load();
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
}
