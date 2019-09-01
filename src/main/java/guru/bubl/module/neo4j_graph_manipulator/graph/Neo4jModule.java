/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import guru.bubl.module.model.FriendlyResourceFactory;
import guru.bubl.module.model.WholeGraph;
import guru.bubl.module.model.admin.WholeGraphAdmin;
import guru.bubl.module.model.center_graph_element.CenterGraphElementOperator;
import guru.bubl.module.model.center_graph_element.CenterGraphElementOperatorFactory;
import guru.bubl.module.model.center_graph_element.CenterGraphElementsOperatorFactory;
import guru.bubl.module.model.center_graph_element.CenteredGraphElementsOperator;
import guru.bubl.module.model.graph.FriendlyResourceOperator;
import guru.bubl.module.model.graph.GraphElementOperator;
import guru.bubl.module.model.graph.GraphElementOperatorFactory;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.edge.EdgeFactory;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.identification.IdentificationFactory;
import guru.bubl.module.model.graph.identification.IdentificationOperator;
import guru.bubl.module.model.graph.schema.SchemaList;
import guru.bubl.module.model.graph.schema.SchemaOperator;
import guru.bubl.module.model.graph.subgraph.SubGraphForker;
import guru.bubl.module.model.graph.subgraph.SubGraphForkerFactory;
import guru.bubl.module.model.graph.vertex.VertexFactory;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphOperator;
import guru.bubl.module.model.meta.UserMetasOperator;
import guru.bubl.module.model.meta.UserMetasOperatorFactory;
import guru.bubl.module.model.test.GraphComponentTest;
import guru.bubl.module.neo4j_graph_manipulator.graph.admin.WholeGraphAdminNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element.CenterGraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element.CenterGraphElementsOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.*;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema.SchemaExtractorFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.SubGraphExtractorFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.IdentificationNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema.SchemaFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema.SchemaListNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema.SchemaOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.subgraph.SubGraphForkerNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexInSubGraphOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.ImageFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.meta.IdentificationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.meta.UserMetasOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.search.GraphSearchModuleNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.test.GraphComponentTestNeo4j;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.configuration.BoltConnector;

import javax.inject.Singleton;
import java.io.File;

public class Neo4jModule extends AbstractModule {

    public static final String
            DB_PATH = "/var/lib/triple_brain/neo4j/db",
            DB_PATH_FOR_TESTS = "/tmp/triple_brain/neo4j/db";

    private Boolean useEmbedded, test;
    private String dbUser, dbPassword;

    public static Neo4jModule forTestingUsingRest() {
        return new Neo4jModule(false, true);
    }

    public static Neo4jModule notForTestingUsingRest() {
        return new Neo4jModule(false, false);
    }

    public static Neo4jModule notForTestingUsingEmbedded(String dbUser, String dbPassword) {
        return new Neo4jModule(true, false);
    }

    public static Neo4jModule forTestingUsingEmbedded() {
        return new Neo4jModule(true, true);
    }

    protected Neo4jModule(Boolean useEmbedded, Boolean test) {
        this.useEmbedded = useEmbedded;
        this.test = test;
    }

    protected Neo4jModule(Boolean useEmbedded, Boolean test, String dbUser, String dbPassword) {
        this.useEmbedded = useEmbedded;
        this.test = test;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    @Override
    protected void configure() {
        bindForEmbedded();
        if (test) {
            bind(GraphComponentTest.class).to(GraphComponentTestNeo4j.class);
        }

        install(new GraphSearchModuleNeo4j());

        bind(WholeGraph.class).to(WholeGraphNeo4j.class);
        bind(WholeGraphAdmin.class).to(WholeGraphAdminNeo4j.class);
        FactoryModuleBuilder factoryModuleBuilder = new FactoryModuleBuilder();

        install(factoryModuleBuilder
                .implement(CenteredGraphElementsOperator.class, CenterGraphElementsOperatorNeo4j.class)
                .build(CenterGraphElementsOperatorFactory.class));

        install(factoryModuleBuilder
                .implement(CenterGraphElementOperator.class, CenterGraphElementOperatorNeo4j.class)
                .build(CenterGraphElementOperatorFactory.class));

        install(factoryModuleBuilder
                .implement(UserMetasOperator.class, UserMetasOperatorNeo4j.class)
                .build(UserMetasOperatorFactory.class));

        install(factoryModuleBuilder
                .build(EdgeFactoryNeo4j.class));

        install(factoryModuleBuilder
                .build(UserGraphFactoryNeo4j.class));

        install(factoryModuleBuilder
                .implement(VertexInSubGraphOperator.class, VertexInSubGraphOperatorNeo4j.class)
                .build(VertexFactory.class));

        bind(
                SchemaList.class
        ).to(
                SchemaListNeo4j.class
        ).in(
                Singleton.class
        );
        install(factoryModuleBuilder
                .implement(SchemaOperator.class, SchemaOperatorNeo4j.class)
                .build(SchemaFactory.class));

        install(factoryModuleBuilder
                .build(VertexFactoryNeo4j.class));

        install(factoryModuleBuilder
                .implement(SubGraphForker.class, SubGraphForkerNeo4j.class)
                .build(SubGraphForkerFactory.class));

        install(factoryModuleBuilder
                .build(SubGraphExtractorFactoryNeo4j.class));

        install(factoryModuleBuilder
                .build(SchemaExtractorFactoryNeo4j.class));


        install(factoryModuleBuilder
                .implement(EdgeOperator.class, EdgeOperatorNeo4j.class)
                .build(EdgeFactory.class));

        install(factoryModuleBuilder
                .implement(GraphElementOperator.class, GraphElementOperatorNeo4j.class)
                .build(GraphElementOperatorFactory.class)
        );

        install(factoryModuleBuilder
                .build(GraphElementFactoryNeo4j.class));

        install(factoryModuleBuilder
                .implement(FriendlyResourceOperator.class, FriendlyResourceNeo4j.class)
                .build(FriendlyResourceFactory.class)
        );
        install(factoryModuleBuilder
                .build(FriendlyResourceFactoryNeo4j.class)
        );
        install(factoryModuleBuilder
                .build(ImageFactoryNeo4j.class)
        );

        install(factoryModuleBuilder
                .implement(IdentificationOperator.class, IdentificationNeo4j.class)
                .build(IdentificationFactory.class)
        );
        install(factoryModuleBuilder
                .build(IdentificationFactoryNeo4j.class)
        );
        bind(GraphFactory.class).to(GraphFactoryNeo4j.class).in(Singleton.class);
    }

    private void bindForEmbedded() {
        BoltConnector boltConnector = new BoltConnector("bolt");
        GraphDatabaseService graphDb;
        Driver driver;
        if (test) {
            graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(
                    new File(test ? DB_PATH_FOR_TESTS : DB_PATH)
            )
                    .setConfig(boltConnector.enabled, "true")
                    .setConfig(boltConnector.type, "BOLT")
                    .setConfig(boltConnector.listen_address, "localhost:7687")
                    .newGraphDatabase();
            bind(GraphDatabaseService.class).toInstance(graphDb);
            Transaction tx = graphDb.beginTx();
            graphDb.execute("CREATE CONSTRAINT ON (n:Resource) ASSERT n.uri IS UNIQUE");
            graphDb.execute("CREATE CONSTRAINT ON (n:User) ASSERT n.email IS UNIQUE");
            graphDb.execute("CREATE INDEX ON :GraphElement(owner)");
            graphDb.execute("CALL db.index.fulltext.createNodeIndex('graphElementLabel',['GraphElement'],['label'])");
            graphDb.execute("CALL db.index.fulltext.createNodeIndex('vertexLabel',['Vertex'],['label'])");
            graphDb.execute("CALL db.index.fulltext.createNodeIndex('username',['User'],['username'])");
            graphDb.execute("CREATE INDEX ON :GraphElement(shareLevel)");
            graphDb.execute("CREATE INDEX ON :GraphElement(last_center_date)");
            graphDb.execute("CREATE INDEX ON :Meta(external_uri)");
            tx.success();
            tx.close();
            registerShutdownHook(graphDb);
            driver = GraphDatabase.driver(
                    "bolt://localhost:7687",
                    AuthTokens.basic("user", "password")
            );
        } else {
            driver = GraphDatabase.driver(
                    "bolt://localhost:7687",
                    AuthTokens.basic(this.dbUser, this.dbPassword)
            );
        }

        bind(Session.class).toInstance(
                driver.session()
        );
    }

    private void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
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
