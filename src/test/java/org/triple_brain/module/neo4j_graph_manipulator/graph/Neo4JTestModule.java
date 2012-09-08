package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.factory.GraphDatabaseSetting;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.triple_brain.module.model.graph.GraphMaker;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JTestModule extends AbstractModule {

    @Override
    protected void configure() {

        GraphDatabaseService graphDb = new TestGraphDatabaseFactory()
                .newImpermanentDatabaseBuilder()
                .setConfig(GraphDatabaseSettings.node_keys_indexable, Neo4JUserGraph.URI_PROPERTY_NAME)
                .setConfig(GraphDatabaseSettings.node_auto_indexing, GraphDatabaseSetting.TRUE)
                .newGraphDatabase();

        bind(GraphDatabaseService.class).toInstance(
                graphDb
        );

        bind(new TypeLiteral<ReadableIndex<Node>>() {
        }).toInstance(
                graphDb.index()
                        .getNodeAutoIndexer()
                        .getAutoIndex()
        );

        install(new FactoryModuleBuilder()
                .build(Neo4JUserGraphFactory.class));

        bind(GraphMaker.class).to(Neo4JGraphMaker.class);
    }
}
