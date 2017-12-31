/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package learning;

import com.google.inject.Guice;
import com.google.inject.Injector;
import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jModule;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.Statement;

public class Neo4jServerTestGeneric {

    protected static Injector injector;

    @Inject
    static protected GraphDatabaseService graphDatabaseService;

    @Inject
    Connection connection;

    protected Transaction transaction;

    @BeforeClass
    public static void realBeforeClass() {
        injector = Guice.createInjector(
                Neo4jModule.forTestingUsingEmbedded()
        );
        graphDatabaseService = injector.getInstance(GraphDatabaseService.class);
    }

    @Before
    public void before() {
        injector.injectMembers(this);
        transaction = graphDatabaseService.beginTx();
        removeEverything();
    }


    @After
    public void after(){
        transaction.close();
    }

    @AfterClass
    public static void afterClass() {
        graphDatabaseService.shutdown();
        Neo4jModule.clearDb();
    }

    private void removeEverything() {
        NoEx.wrap(() -> {
                    String query = "START n = node(*) OPTIONAL MATCH n-[r]-() DELETE n, r;";
                    Statement stmt = connection.createStatement();
                    return stmt.executeQuery(
                            query
                    );
                }
        );
    }

}
