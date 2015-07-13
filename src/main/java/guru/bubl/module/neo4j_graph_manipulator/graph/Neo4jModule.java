/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import guru.bubl.module.model.EmptyGraphTransaction;
import guru.bubl.module.model.FriendlyResourceFactory;
import guru.bubl.module.model.GraphTransaction;
import guru.bubl.module.model.WholeGraph;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.graph.edge.EdgeFactory;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.schema.SchemaOperator;
import guru.bubl.module.model.graph.vertex.VertexFactory;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphOperator;
import guru.bubl.module.model.test.GraphComponentTest;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.*;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema.Neo4jSchemaExtractorFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.Neo4jSubGraphExtractorFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema.Neo4jSchemaOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema.SchemaFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.Neo4jImageFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.test.Neo4JGraphComponentTest;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.helpers.collection.MapUtil;

import javax.inject.Singleton;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Neo4jModule extends AbstractModule {

    public static final String DB_PATH = "/var/lib/triple_brain/neo4j/db";
    public static final String DB_PATH_FOR_TESTS = "/tmp/triple_brain/neo4j/db";

    private Boolean useEmbedded;
    private Boolean test;

    public static Neo4jModule forTestingUsingRest() {
        return new Neo4jModule(false, true);
    }

    public static Neo4jModule notForTestingUsingRest() {
        return new Neo4jModule(false, false);
    }

    public static Neo4jModule notForTestingUsingEmbedded() {
        return new Neo4jModule(true, false);
    }

    public static Neo4jModule forTestingUsingEmbedded() {
        return new Neo4jModule(true, true);
    }

    protected Neo4jModule(Boolean useEmbedded, Boolean test) {
        this.useEmbedded = useEmbedded;
        this.test = test;
    }

    @Override
    protected void configure() {
        if (useEmbedded) {
            bindForEmbedded();
        } else {
            bindForRestApi();
        }
        if (test) {
            bind(GraphComponentTest.class).to(Neo4JGraphComponentTest.class);
        }

        bind(WholeGraph.class).to(Neo4jWholeGraph.class);

        FactoryModuleBuilder factoryModuleBuilder = new FactoryModuleBuilder();

        install(factoryModuleBuilder
                .build(Neo4jEdgeFactory.class));

        install(factoryModuleBuilder
                .build(Neo4jUserGraphFactory.class));

        install(factoryModuleBuilder
                .implement(VertexInSubGraphOperator.class, Neo4jVertexInSubGraphOperator.class)
                .build(VertexFactory.class));

        install(factoryModuleBuilder
                .implement(SchemaOperator.class, Neo4jSchemaOperator.class)
                .build(SchemaFactory.class));

        install(factoryModuleBuilder
                .build(Neo4jVertexFactory.class));

        install(factoryModuleBuilder
                .build(Neo4jSubGraphExtractorFactory.class));

        install(factoryModuleBuilder
                .build(Neo4jSchemaExtractorFactory.class));


        install(factoryModuleBuilder
                .implement(EdgeOperator.class, Neo4jEdgeOperator.class)
                .build(EdgeFactory.class));

        install(factoryModuleBuilder
                        .implement(GraphElementOperator.class, Neo4jGraphElementOperator.class)
                        .build(GraphElementOperatorFactory.class)
        );

        install(factoryModuleBuilder
                .build(Neo4jGraphElementFactory.class));

        install(factoryModuleBuilder
                        .implement(FriendlyResourceOperator.class, Neo4jFriendlyResource.class)
                        .build(FriendlyResourceFactory.class)
        );
        install(factoryModuleBuilder
                        .build(Neo4jFriendlyResourceFactory.class)
        );
        install(factoryModuleBuilder
                        .build(Neo4jImageFactory.class)
        );

        install(factoryModuleBuilder
                        .implement(IdentificationOperator.class, Neo4jIdentification.class)
                        .build(IdentificationFactory.class)
        );
        install(factoryModuleBuilder
                        .build(Neo4jIdentificationFactory.class)
        );
        bind(GraphFactory.class).to(Neo4jGraphFactory.class).in(Singleton.class);
    }

    private void bindForEmbedded() {
        bind(GraphTransaction.class).to(Neo4jGraphTransaction.class);
        // Make sure Neo4j Driver is registered
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(
                test ? DB_PATH_FOR_TESTS : DB_PATH
        )
                .setConfig(
                        GraphDatabaseSettings.node_keys_indexable,
                        Neo4jFriendlyResource.props.uri + "," +
                                Neo4jFriendlyResource.props.label + "," +
                                Neo4jIdentification.props.external_uri + "," +
                                Neo4jFriendlyResource.props.owner + "," +
                                Neo4jFriendlyResource.props.type + "," +
                                Neo4jVertexInSubGraphOperator.props.is_public + "," +
                                "email"
                ).setConfig(
                        GraphDatabaseSettings.node_auto_indexing,
                        "true"
                ).newGraphDatabase();
        Transaction tx = graphDb.beginTx();
        graphDb.index().forNodes("node_auto_index",
                MapUtil.stringMap(
                        IndexManager.PROVIDER,
                        "lucene",
                        "type",
                        "fulltext",
                        "to_lower_case",
                        "true"
                )
        );
        bind(Connection.class).toInstance(
                getConnectionForEmbedded(graphDb)
        );
        if (test) {
            registerShutdownHook(graphDb);
        }
        bind(new TypeLiteral<ReadableIndex<Node>>() {
        }).toInstance(
                graphDb.index()
                        .getNodeAutoIndexer()
                        .getAutoIndex()
        );
        tx.success();
        tx.close();
        bind(GraphDatabaseService.class).toInstance(
                graphDb
        );
    }

    private void bindForRestApi() {
        bind(GraphTransaction.class).to(
                EmptyGraphTransaction.class
        );
        bind(Connection.class).toInstance(
                getConnectionForRest()
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

    private Connection getConnectionForEmbedded(GraphDatabaseService graphDatabaseService) {
        try {
            Class.forName("org.neo4j.jdbc.Driver");
            Properties props = new Properties();
            props.put("bubl.guru_db", graphDatabaseService);
            return DriverManager.getConnection(
                    "jdbc:neo4j:instance:bubl.guru_db",
                    props
            );
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnectionForRest() {
        try {
            Class.forName("org.neo4j.jdbc.Driver");
            return DriverManager.getConnection(
                    "jdbc:neo4j:" +
                            (test ?
                                    "//localhost:9594/db/data?debug=true" :
                                    "//localhost:8484/db/data"
                            )
            );
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
