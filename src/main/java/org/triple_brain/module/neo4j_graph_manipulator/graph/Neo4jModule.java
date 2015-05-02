/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.impl.util.FileUtils;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestAPIFacade;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.triple_brain.module.model.EmptyGraphTransaction;
import org.triple_brain.module.model.FriendlyResourceFactory;
import org.triple_brain.module.model.GraphTransaction;
import org.triple_brain.module.model.WholeGraph;
import org.triple_brain.module.model.graph.*;
import org.triple_brain.module.model.graph.edge.EdgeFactory;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.schema.SchemaOperator;
import org.triple_brain.module.model.graph.vertex.VertexFactory;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphOperator;
import org.triple_brain.module.model.test.GraphComponentTest;
import org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.QueryEngineUsingEmbedded;
import org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.RestApiUsingEmbedded;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.*;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.schema.Neo4jSchemaExtractorFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.Neo4jSubGraphExtractorFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.schema.Neo4jSchemaOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.schema.SchemaFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.image.Neo4jImageFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.test.Neo4JGraphComponentTest;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;

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
        if (test) {
            System.setProperty(
                    "org.neo4j.rest.logging_filter",
                    "true"
            );
            System.setProperty(
                    "org.neo4j.rest.batch_transaction",
                    "false"
            );
        }
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
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(
                test ? DB_PATH_FOR_TESTS : DB_PATH
        )
                .setConfig(
                        GraphDatabaseSettings.node_keys_indexable,
                        Neo4jFriendlyResource.props.uri + "," +
                                Neo4jFriendlyResource.props.label + "," +
                                Neo4jFriendlyResource.props.owner + "," +
                                Neo4jFriendlyResource.props.type + "," +
                                Neo4jVertexInSubGraphOperator.props.is_public + "," +
                                Neo4jIdentification.props.external_uri + "," +
                                "email"
                ).setConfig(
                        GraphDatabaseSettings.node_auto_indexing,
                        "true"
                ).newGraphDatabase();

        RestAPI restAPI = RestApiUsingEmbedded.usingGraphDb(
                graphDb
        );
        Transaction tx = restAPI.beginTx();
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
        bind(QueryEngine.class).toInstance(
                QueryEngineUsingEmbedded.usingGraphDb(
                        graphDb
                )
        );
        bind(RestAPI.class).toInstance(
                restAPI
        );
        bind(GraphDatabaseService.class).toInstance(
                graphDb
        );
    }

    private void bindForRestApi() {
        bind(GraphTransaction.class).to(
                EmptyGraphTransaction.class
        );
        RestAPI restApi = new RestAPIFacade(
                "http://localhost:8484/db/data"
        );
        bind(
                RestAPI.class
        ).toInstance(restApi);
        bind(GraphDatabaseService.class).toInstance(
                new RestGraphDatabase(
                        restApi
                )
        );
        bind(QueryEngine.class).toInstance(
                new RestCypherQueryEngine(restApi)
        );
        bind(new TypeLiteral<ReadableIndex<Node>>() {
        }).toInstance(
                restApi.index()
                        .getNodeAutoIndexer()
                        .getAutoIndex()
        );

//        bind(new TypeLiteral<ReadableIndex<Relationship>>() {
//        }).toInstance(
//                restApi.index()
//                        .getRelationshipAutoIndexer()
//                        .getAutoIndex()
//        );
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
        try {
            FileUtils.deleteRecursively(new File(
                    DB_PATH_FOR_TESTS
            ));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
