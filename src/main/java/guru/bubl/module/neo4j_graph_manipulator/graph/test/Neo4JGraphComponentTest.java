/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.test;

import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.SubGraphPojo;
import guru.bubl.module.model.graph.UserGraph;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgeFactory;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphOperator;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.test.GraphComponentTest;
import guru.bubl.module.model.test.SubGraphOperator;
import guru.bubl.module.model.test.scenarios.TestScenarios;
import guru.bubl.module.model.test.scenarios.VerticesCalledABAndC;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jModule;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraphFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jWholeGraph;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.Neo4jSubGraphExtractorFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Neo4JGraphComponentTest implements GraphComponentTest {

    @Inject
    protected TestScenarios testScenarios;

    @Inject
    protected Neo4jSubGraphExtractorFactory neo4jSubGraphExtractorFactory;

    @Inject
    Neo4jWholeGraph wholeGraph;

    @Inject
    GraphDatabaseService graphDatabaseService;

    @Inject
    protected EdgeFactory edgeFactory;

    @Inject
    protected Neo4jVertexFactory vertexFactory;

    @Inject
    protected Neo4jUserGraphFactory neo4jUserGraphFactory;

    @Inject
    protected GraphFactory graphFactory;

    @Inject
    Connection connection;

    protected VertexOperator vertexA;
    protected VertexOperator vertexB;
    protected VertexOperator vertexC;

    protected static User user;

    protected static User anotherUser;
    protected static UserGraph anotherUserGraph;
    protected static VertexOperator vertexOfAnotherUser;

    protected Transaction transaction;

    protected UserGraph userGraph;

    @Override
    public void beforeClass() {
    }

    @Override
    public void before() {
        transaction = graphDatabaseService.beginTx();
        user = User.withEmail(
                "roger.lamothe@example.org"
        ).setUsername("roger_lamothe");
        anotherUser = User.withEmail(
                "colette.armande@example.org"
        ).setUsername("colette_armande");

        userGraph = neo4jUserGraphFactory.withUser(user);
        VerticesCalledABAndC verticesCalledABAndC = testScenarios.makeGraphHave3VerticesABCWhereAIsDefaultCenterVertexAndAPointsToBAndBPointsToC(
                userGraph
        );
        vertexA = verticesCalledABAndC.vertexA();
        vertexB = verticesCalledABAndC.vertexB();
        vertexC = verticesCalledABAndC.vertexC();
        anotherUserGraph = neo4jUserGraphFactory.withUser(anotherUser);
        graphFactory.createForUser(anotherUserGraph.user());
        vertexOfAnotherUser = anotherUserGraph.defaultVertex();
        vertexOfAnotherUser.label("vertex of another user");
    }

    @Override
    public void after() {
        transaction.failure();
        transaction.close();
    }

    @Override
    public void afterClass() {
        graphDatabaseService.shutdown();
        Neo4jModule.clearDb();
    }

    @Override
    public int numberOfEdgesAndVertices() {
        return numberOfVertices() +
                numberOfEdges();
    }

    @Override
    public SubGraphPojo wholeGraphAroundDefaultCenterVertex() {
        Integer depthThatShouldCoverWholeGraph = 1000;
        return neo4jSubGraphExtractorFactory.withCenterVertexAndDepth(
                vertexA.uri(),
                depthThatShouldCoverWholeGraph
        ).load();
    }


    @Override
    public SubGraphOperator wholeGraph() {
        return SubGraphOperator.withVerticesAndEdges(
                wholeGraph.getAllVertices(),
                wholeGraph.getAllEdges()
        );
    }

    @Override
    public void removeWholeGraph() {
        NoExRun.wrap(() -> {
                    Statement stmt = connection.createStatement();
                    return stmt.executeQuery(
                            String.format(
                                    "START n=node:node_auto_index('type:%s OR type:%s OR type:%s OR type:%s'), " +
                                            "r=relationship(*) DELETE n, r;",
                                    GraphElementType.vertex,
                                    GraphElementType.edge,
                                    GraphElementType.schema,
                                    GraphElementType.property
                            )
                    );
                }
        ).get();
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public void user(User user) {
        this.user = user;
    }

    @Override
    public UserGraph userGraph() {
        return userGraph;
    }

    @Override
    public VertexOperator vertexA() {
        return vertexFactory.withUri(
                vertexA.uri()
        );
    }

    @Override
    public void setDefaultVertexAkaVertexA(VertexOperator vertexA) {
        this.vertexA = vertexA;
    }

    @Override
    public VertexOperator vertexB() {
        return vertexFactory.withUri(
                vertexB.uri()
        );
    }

    @Override
    public VertexOperator vertexC() {
        return vertexFactory.withUri(
                vertexC.uri()
        );
    }

    @Override
    public VertexOperator vertexOfAnotherUser() {
        return vertexOfAnotherUser;
    }

    @Override
    public VertexInSubGraphPojo vertexInWholeConnectedGraph(Vertex vertex) {
        return (VertexInSubGraphPojo) wholeGraphAroundDefaultCenterVertex().vertexWithIdentifier(
                vertex.uri()
        );
    }

    @Override
    public EdgePojo edgeInWholeGraph(Edge edge) {
        return (EdgePojo) wholeGraphAroundDefaultCenterVertex().edgeWithIdentifier(
                edge.uri()
        );
    }

    protected int numberOfVertices() {
        return wholeGraph.getAllVertices().size();
    }

    protected int numberOfEdges() {
        return wholeGraph.getAllEdges().size();
    }
}
