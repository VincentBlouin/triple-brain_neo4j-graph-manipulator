/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;

import java.util.Map;

public interface Neo4jOperator {
    String queryPrefix();
    Node getNode();
    Map<String,Object> addCreationProperties(Map<String,Object> map);
}
