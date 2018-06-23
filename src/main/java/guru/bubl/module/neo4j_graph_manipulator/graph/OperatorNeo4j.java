/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import guru.bubl.module.common_utils.NamedParameterStatement;
import org.neo4j.graphdb.Node;

import java.sql.SQLException;
import java.util.Map;

public interface OperatorNeo4j {
    String queryPrefix();
    Node getNode();
    Map<String,Object> addCreationProperties(Map<String,Object> map);
    void setNamedCreationProperties(NamedParameterStatement statement) throws SQLException;
}
