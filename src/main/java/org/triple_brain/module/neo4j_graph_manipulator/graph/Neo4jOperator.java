package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;

import java.util.Map;

/*
 * Copyright Mozilla Public License 1.1
 */
public interface Neo4jOperator {
    String queryPrefix();
    Node getNode();
    Map<String,Object> addCreationProperties(Map<String,Object> map);
}
