/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import com.google.inject.AbstractModule;
import guru.bubl.module.model.search.GraphIndexer;
import guru.bubl.module.model.search.GraphSearch;

public class Neo4jGraphSearchModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(GraphIndexer.class).to(Neo4jGraphIndexer.class);
        bind(GraphSearch.class).to(Neo4jGraphSearch.class);
    }
}
