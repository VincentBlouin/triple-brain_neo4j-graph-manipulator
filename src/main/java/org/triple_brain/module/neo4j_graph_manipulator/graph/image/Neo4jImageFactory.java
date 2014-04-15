package org.triple_brain.module.neo4j_graph_manipulator.graph.image;

import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;

public interface Neo4jImageFactory {
    Neo4jImages forResource(Neo4jFriendlyResource friendlyResource);
}
