/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package learning;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jModule;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Ignore("I want to prevent starting another noe4j server here to prevent memory overload")
public class Neo4JTest {

    private static enum RelTypes implements RelationshipType {
        KNOWS
    }

    GraphDatabaseService graphDb;
    Node firstNode;
    Node secondNode;
    Relationship relationship;
    String DB_PATH = "src/test/resources/learning/db";
    String greeting;

    @Test
    public void helloWorld() {
        createDb();
        assertThat(greeting, is("Hello, brave Neo4j World!"));
        removeData();
        shutDown();
    }

    void createDb() {
        clearDb();
        // START SNIPPET: startDb
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
        registerShutdownHook(graphDb);
        // END SNIPPET: startDb

        // START SNIPPET: transaction
        Transaction tx = graphDb.beginTx();
        try {
            // Mutating operations go here
            // END SNIPPET: transaction
            // START SNIPPET: addData
            firstNode = graphDb.createNode();
            firstNode.setProperty("message", "Hello, ");
            secondNode = graphDb.createNode();
            secondNode.setProperty("message", "World!");

            relationship = firstNode.createRelationshipTo(secondNode, RelTypes.KNOWS);
            relationship.setProperty("message", "brave Neo4j ");
            // END SNIPPET: addData

            // START SNIPPET: readData
            System.out.print(firstNode.getProperty("message"));
            System.out.print(relationship.getProperty("message"));
            System.out.print(secondNode.getProperty("message"));
            // END SNIPPET: readData

            greeting = (String) (firstNode.getProperty("message"))
                    + (relationship.getProperty("message"))
                    + (secondNode.getProperty("message"));

            // START SNIPPET: transaction
            tx.success();
        } finally {
            tx.finish();
        }
        // END SNIPPET: transaction
    }

    private void clearDb() {
        Neo4jModule.deleteFileOrDirectory(
                new File(DB_PATH)
        );
    }

    void removeData() {
        Transaction tx = graphDb.beginTx();
        try {
            // START SNIPPET: removingData
            // let's remove the data
            firstNode.getSingleRelationship(RelTypes.KNOWS, Direction.OUTGOING).delete();
            firstNode.delete();
            secondNode.delete();
            // END SNIPPET: removingData

            tx.success();
        } finally {
            tx.finish();
        }
    }

    void shutDown() {
        System.out.println();
        System.out.println("Shutting down database ...");
        // START SNIPPET: shutdownServer
        graphDb.shutdown();
        // END SNIPPET: shutdownServer
    }

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }
}
