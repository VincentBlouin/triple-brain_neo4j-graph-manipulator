/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph;

import org.triple_brain.module.model.User;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;

public interface Neo4jUserGraphFactory {
    Neo4jUserGraph withUser(User user);
}
