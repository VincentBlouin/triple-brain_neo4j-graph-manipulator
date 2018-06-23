/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.model.User;

public interface UserGraphFactoryNeo4j {
    UserGraphNeo4j withUser(User user);
}
