package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin;

import apoc.create.Create;
import apoc.meta.Meta;
import apoc.path.PathExplorer;
import apoc.refactor.GraphRefactoring;
import com.google.inject.AbstractModule;
import guru.bubl.module.model.WholeGraph;
import guru.bubl.module.model.test.GraphComponentTest;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.exceptions.KernelException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import java.io.File;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;
public class Neo4jModuleForTests extends AbstractModule {

    public static final String DB_PATH_FOR_TESTS = "/tmp/triple_brain/neo4j/db";

    @Override
    protected void configure() {
        bindForEmbedded();
    }

    private void bindForEmbedded() {
        GraphDatabaseService graphDb;
        Driver driver;
        DatabaseManagementService databaseManagementService = new DatabaseManagementServiceBuilder(new File(DB_PATH_FOR_TESTS))
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, new SocketAddress("localhost", 7687))
                .setConfig(GraphDatabaseSettings.cypher_lenient_create_relationship, true)
//                    .setConfig(GraphDatabaseSettings.procedure_unrestricted, List.of(
//                            "apoc.*"
//                    ))
                .build();
        graphDb = databaseManagementService.database(DEFAULT_DATABASE_NAME);
        registerProcedure(graphDb, PathExplorer.class);
        registerProcedure(graphDb, GraphRefactoring.class);
        registerProcedure(graphDb, Meta.class);
        registerProcedure(graphDb, Create.class);
        bind(GraphDatabaseService.class).toInstance(graphDb);
        Transaction tx = graphDb.beginTx();
        graphDb.executeTransactionally("CREATE INDEX ON :Resource(uri)");
        graphDb.executeTransactionally("CREATE CONSTRAINT ON (n:User) ASSERT n.email IS UNIQUE");
        graphDb.executeTransactionally("CREATE INDEX ON :GraphElement(owner)");
        graphDb.executeTransactionally("CALL db.index.fulltext.createNodeIndex('graphElementLabel',['GraphElement'],['label'])");
        graphDb.executeTransactionally("CALL db.index.fulltext.createNodeIndex('vertexLabel',['Vertex'],['label'])");
        graphDb.executeTransactionally("CALL db.index.fulltext.createNodeIndex('tagLabel',['Meta'],['label'])");
        graphDb.executeTransactionally("CALL db.index.fulltext.createNodeIndex('patternLabel',['Pattern'],['label'])");
        graphDb.executeTransactionally("CALL db.index.fulltext.createNodeIndex('username',['User'],['username'])");
        graphDb.executeTransactionally("CREATE INDEX ON :GraphElement(shareLevel)");
        graphDb.executeTransactionally("CREATE INDEX ON :GraphElement(last_center_date)");
        graphDb.executeTransactionally("CREATE INDEX ON :Meta(external_uri)");
        graphDb.executeTransactionally("CREATE INDEX ON :GraphElement(isUnderPattern)");
        graphDb.executeTransactionally("CREATE INDEX ON :GraphElement(nb_visits)");
        graphDb.executeTransactionally("CREATE INDEX ON :GraphElement(nb_private_neighbors)");
        graphDb.executeTransactionally("CREATE INDEX ON :GraphElement(nb_friend_neighbors)");
        graphDb.executeTransactionally("CREATE INDEX ON :GraphElement(nb_public_neighbors)");
        tx.commit();
        tx.close();
        registerShutdownHook(graphDb);
        driver = GraphDatabase.driver(
                "bolt://localhost:7687",
                AuthTokens.basic("user", "password")
        );

        bind(Driver.class).toInstance(
                driver
        );
        bind(GraphComponentTest.class).to(GraphComponentTestNeo4j.class);
        bind(WholeGraph.class).to(WholeGraphNeo4j.class);
    }

    public static void registerProcedure(GraphDatabaseService db, Class<?>... procedures) {
        GlobalProcedures globalProcedures = ((GraphDatabaseAPI) db).getDependencyResolver().resolveDependency(GlobalProcedures.class);
        for (Class<?> procedure : procedures) {
            try {
                globalProcedures.registerProcedure(procedure, true);
                globalProcedures.registerFunction(procedure, true);
                globalProcedures.registerAggregationFunction(procedure, true);
            } catch (KernelException e) {
                throw new RuntimeException("while registering " + procedure, e);
            }
        }
    }

    private void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                clearDb();
            }
        });
    }

    public static void clearDb() {
        deleteFileOrDirectory(
                new File(
                        DB_PATH_FOR_TESTS
                )
        );
    }

    public static void deleteFileOrDirectory(final File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    deleteFileOrDirectory(child);
                }
            }
            file.delete();
        }
    }
}
