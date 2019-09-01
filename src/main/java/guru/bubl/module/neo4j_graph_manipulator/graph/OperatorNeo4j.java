/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import java.util.Map;

public interface OperatorNeo4j {
    String queryPrefix();
    Map<String,Object> addCreationProperties(Map<String,Object> map);
}
