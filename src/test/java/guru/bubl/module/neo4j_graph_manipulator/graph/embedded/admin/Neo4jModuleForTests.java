package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin;

import apoc.create.Create;
import apoc.meta.Meta;
import apoc.path.PathExplorer;
import apoc.refactor.GraphRefactoring;
import com.google.inject.AbstractModule;
import guru.bubl.module.model.test.GraphComponentTest;
import guru.bubl.module.neo4j_graph_manipulator.graph.test.GraphComponentTestNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.test.SetupNeo4jDatabaseForTests;
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
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import java.io.File;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jModule.DB_PATH_FOR_TESTS;
import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jModule.NEO4J_PASSWORD_FOR_TESTS;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class Neo4jModuleForTests extends AbstractModule {

    private static final Integer BOLT_PORT_FOR_EMBEDDED = 8099;

    private Boolean useEmbedded;

    public static Neo4jModuleForTests usingEmbedded() {
        return new Neo4jModuleForTests(true);
    }

    public static Neo4jModuleForTests notUsingEmbedded() {
        return new Neo4jModuleForTests(false);
    }

    protected Neo4jModuleForTests(Boolean useEmbedded) {
        this.useEmbedded = useEmbedded;
    }

    @Override
    protected void configure() {
        if (useEmbedded) {
            bindForEmbedded();
        }
        bind(GraphComponentTest.class).to(GraphComponentTestNeo4j.class);
    }

    private void bindForEmbedded() {
        GraphDatabaseService graphDb;
        Driver driver;
        DatabaseManagementService databaseManagementService = new DatabaseManagementServiceBuilder(new File(DB_PATH_FOR_TESTS))
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, new SocketAddress("localhost", BOLT_PORT_FOR_EMBEDDED))
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
        registerShutdownHook(graphDb);
        driver = GraphDatabase.driver(
                "bolt://localhost:" + BOLT_PORT_FOR_EMBEDDED,
                AuthTokens.basic("neo4j", NEO4J_PASSWORD_FOR_TESTS)
        );

        bind(Driver.class).toInstance(
                driver
        );
        new SetupNeo4jDatabaseForTests().doItWithDriver(driver);
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
