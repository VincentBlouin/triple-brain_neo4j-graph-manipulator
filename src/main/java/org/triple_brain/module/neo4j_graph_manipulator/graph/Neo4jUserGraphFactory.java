package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.triple_brain.module.model.User;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4jUserGraphFactory {
    public Neo4jUserGraph withUser(User user);
}
