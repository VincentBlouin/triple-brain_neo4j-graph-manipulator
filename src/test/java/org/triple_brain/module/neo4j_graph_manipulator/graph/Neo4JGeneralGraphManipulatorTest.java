package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.graph.Vertex;
import org.triple_brain.module.model.graph.scenarios.TestScenarios;

import javax.inject.Inject;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JGeneralGraphManipulatorTest {

    @Inject
    protected TestScenarios testScenarios;

    protected Vertex vertexA;
    protected Vertex vertexB;
    protected Vertex vertexC;

    protected static User user;

    protected Transaction transaction;

    @Inject
    protected GraphDatabaseService graphDb;

    @Inject
    protected Neo4JUserGraphFactory neo4JUserGraphFactory;


    Neo4JGraphMaker neo4JGraphMaker;

    private static Injector injector;

    @BeforeClass
    public static void beforeClass() throws Exception {
        injector = Guice.createInjector(new Neo4JTestModule());
        user = User.withUsernameAndEmail(
                "roger_lamothe",
                "roger.lamothe@example.org"
        );
    }

    @Before
    public void before() throws Exception{
        injector.injectMembers(this);
        transaction = graphDb.beginTx();
        testScenarios.makeGraphHave3VerticesABCWhereAIsDefaultCenterVertexAndAPointsToBAndBPointsToC(
                neo4JUserGraphFactory.withUser(user)
        );

    }

    @After
    public void destroyTestDatabase()
    {
        transaction.success();
        graphDb.shutdown();
    }
}
