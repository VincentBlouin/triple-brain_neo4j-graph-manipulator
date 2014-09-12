/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package learning;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jModule;

import javax.inject.Inject;
import java.util.Collections;

public class Neo4jServerTestGeneric {

    protected static Injector injector;

    @Inject
    protected RestAPI restApi;

    @Inject
    protected QueryEngine queryEngine;

    @Inject
    protected ReadableIndex<Node> nodeIndex;

    @Inject
    static protected GraphDatabaseService graphDatabaseService;

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
        transaction = restApi.beginTx();
        removeEverything();
    }

    @After
    public void after(){
        transaction.finish();
    }

    @AfterClass
    public static void afterClass() {
        graphDatabaseService.shutdown();
        Neo4jModule.clearDb();
    }

    private void removeEverything() {
        restApi.query(
                "START n = node(*) OPTIONAL MATCH n-[r]-() DELETE n, r;",
                Collections.EMPTY_MAP
        );
    }

}
