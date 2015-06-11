/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.image;

import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;

public interface Neo4jImageFactory {
    Neo4jImages forResource(Neo4jFriendlyResource friendlyResource);
}
