/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.subgraph.UserGraph;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;

import javax.inject.Inject;

public class GraphFactoryNeo4j implements GraphFactory {

    @Inject
    protected UserGraphFactoryNeo4j neo4jUserGraphFactory;

    @Inject
    protected VertexFactoryNeo4j vertexFactory;

    @Override
    public UserGraph loadForUser(User user) {
        return neo4jUserGraphFactory.withUser(user);
    }

}
