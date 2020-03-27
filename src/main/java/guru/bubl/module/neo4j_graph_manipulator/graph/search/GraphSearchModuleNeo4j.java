/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import guru.bubl.module.model.search.GraphSearch;
import guru.bubl.module.model.search.GraphSearchFactory;

public class GraphSearchModuleNeo4j extends AbstractModule {

    @Override
    protected void configure() {
        FactoryModuleBuilder factoryModuleBuilder = new FactoryModuleBuilder();
        install(factoryModuleBuilder
                .implement(GraphSearch.class, GraphSearchNeo4j.class)
                .build(GraphSearchFactory.class));
    }
}
