/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import guru.bubl.module.model.*;
import guru.bubl.module.model.admin.WholeGraphAdmin;
import guru.bubl.module.model.admin.WholeGraphAdminFactory;
import guru.bubl.module.model.center_graph_element.CenterGraphElementOperator;
import guru.bubl.module.model.center_graph_element.CenterGraphElementOperatorFactory;
import guru.bubl.module.model.center_graph_element.CenterGraphElementsOperatorFactory;
import guru.bubl.module.model.center_graph_element.CenteredGraphElementsOperator;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.graph.edge.EdgeFactory;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.identification.IdentificationFactory;
import guru.bubl.module.model.graph.identification.IdentificationOperator;
import guru.bubl.module.model.graph.schema.SchemaList;
import guru.bubl.module.model.graph.schema.SchemaOperator;
import guru.bubl.module.model.graph.subgraph.SubGraphForker;
import guru.bubl.module.model.graph.subgraph.SubGraphForkerFactory;
import guru.bubl.module.model.graph.vertex.VertexFactory;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphOperator;
import guru.bubl.module.model.meta.IdentifiedTo;
import guru.bubl.module.model.meta.UserMetasOperator;
import guru.bubl.module.model.meta.UserMetasOperatorFactory;
import guru.bubl.module.model.test.GraphComponentTest;
import guru.bubl.module.neo4j_graph_manipulator.graph.admin.Neo4jWholeGraphAdmin;
import guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element.Neo4jCenterGraphElementOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element.Neo4jCenterGraphElementsOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.*;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema.Neo4jSchemaExtractorFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.Neo4jSubGraphExtractorFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.Neo4jIdentification;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema.Neo4jSchemaList;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema.Neo4jSchemaOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema.SchemaFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.subgraph.Neo4jSubGraphForker;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.Neo4jImageFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.meta.IdentifiedToNeo4J;
import guru.bubl.module.neo4j_graph_manipulator.graph.meta.Neo4jIdentificationFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.meta.Neo4jUserMetasOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.test.Neo4JGraphComponentTest;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.BidirectionalTraversalDescription;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.MapUtil;

import javax.inject.Singleton;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

public class Neo4jModule extends AbstractModule {

    public static final String
            DB_PATH = "/var/lib/triple_brain/neo4j/db",
            DB_PATH_FOR_TESTS = "/tmp/triple_brain/neo4j/db";

    private Boolean useEmbedded, test;

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

        bind(IdentifiedTo.class).to(IdentifiedToNeo4J.class);


        FactoryModuleBuilder factoryModuleBuilder = new FactoryModuleBuilder();

        install(factoryModuleBuilder
                .implement(WholeGraphAdmin.class, Neo4jWholeGraphAdmin.class)
                .build(WholeGraphAdminFactory.class));

        install(factoryModuleBuilder
                .implement(CenteredGraphElementsOperator.class, Neo4jCenterGraphElementsOperator.class)
                .build(CenterGraphElementsOperatorFactory.class));

        install(factoryModuleBuilder
                .implement(CenterGraphElementOperator.class, Neo4jCenterGraphElementOperator.class)
                .build(CenterGraphElementOperatorFactory.class));

        install(factoryModuleBuilder
                .implement(UserMetasOperator.class, Neo4jUserMetasOperator.class)
                .build(UserMetasOperatorFactory.class));

        install(factoryModuleBuilder
                .build(Neo4jEdgeFactory.class));

        install(factoryModuleBuilder
                .build(Neo4jUserGraphFactory.class));

        install(factoryModuleBuilder
                .implement(VertexInSubGraphOperator.class, Neo4jVertexInSubGraphOperator.class)
                .build(VertexFactory.class));

        bind(
                SchemaList.class
        ).to(
                Neo4jSchemaList.class
        ).in(
                Singleton.class
        );
        install(factoryModuleBuilder
                .implement(SchemaOperator.class, Neo4jSchemaOperator.class)
                .build(SchemaFactory.class));

        install(factoryModuleBuilder
                .build(Neo4jVertexFactory.class));

        install(factoryModuleBuilder
                .implement(SubGraphForker.class, Neo4jSubGraphForker.class)
                .build(SubGraphForkerFactory.class));

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
                                Neo4jCenterGraphElementOperator.props.last_center_date + "," +
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
        bind(GraphDatabaseService.class).toInstance(graphDb);
        if (test) {
            registerShutdownHook(graphDb);
        }
        tx.success();
        tx.close();
    }

    private void bindForRestApi() {
        bind(GraphTransaction.class).to(
                EmptyGraphTransaction.class
        );
        bind(Connection.class).toInstance(
                getConnectionForRest()
        );
        bind(GraphDatabaseService.class).toInstance(
                dummyGraphDatabaseService()
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

    private GraphDatabaseService dummyGraphDatabaseService() {
        return new GraphDatabaseService() {
            @Override
            public Node createNode() {
                return null;
            }

            @Override
            public Node createNode(Label... labels) {
                return null;
            }

            @Override
            public Node getNodeById(long id) {
                return null;
            }

            @Override
            public Relationship getRelationshipById(long id) {
                return null;
            }

            @Override
            public Iterable<Node> getAllNodes() {
                return null;
            }

            @Override
            public ResourceIterator<Node> findNodes(Label label, String key, Object value) {
                return null;
            }

            @Override
            public Node findNode(Label label, String key, Object value) {
                return null;
            }

            @Override
            public ResourceIterator<Node> findNodes(Label label) {
                return null;
            }

            @Override
            public ResourceIterable<Node> findNodesByLabelAndProperty(Label label, String key, Object value) {
                return null;
            }

            @Override
            public Iterable<RelationshipType> getRelationshipTypes() {
                return null;
            }

            @Override
            public boolean isAvailable(long timeout) {
                return false;
            }

            @Override
            public void shutdown() {

            }

            @Override
            public Transaction beginTx() {
                return null;
            }

            @Override
            public Result execute(String query) throws QueryExecutionException {
                return null;
            }

            @Override
            public Result execute(String query, Map<String, Object> parameters) throws QueryExecutionException {
                return null;
            }

            @Override
            public <T> TransactionEventHandler<T> registerTransactionEventHandler(TransactionEventHandler<T> handler) {
                return null;
            }

            @Override
            public <T> TransactionEventHandler<T> unregisterTransactionEventHandler(TransactionEventHandler<T> handler) {
                return null;
            }

            @Override
            public KernelEventHandler registerKernelEventHandler(KernelEventHandler handler) {
                return null;
            }

            @Override
            public KernelEventHandler unregisterKernelEventHandler(KernelEventHandler handler) {
                return null;
            }

            @Override
            public Schema schema() {
                return null;
            }

            @Override
            public IndexManager index() {
                return null;
            }

            @Override
            public TraversalDescription traversalDescription() {
                return null;
            }

            @Override
            public BidirectionalTraversalDescription bidirectionalTraversalDescription() {
                return null;
            }
        };
    }
}
