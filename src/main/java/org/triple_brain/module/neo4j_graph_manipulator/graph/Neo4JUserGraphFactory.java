package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.triple_brain.module.model.User;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JUserGraphFactory {
    public Neo4JUserGraph withUser(User user);
}
