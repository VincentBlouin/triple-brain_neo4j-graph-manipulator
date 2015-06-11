/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import org.neo4j.graphdb.Node;
import guru.bubl.module.model.graph.GraphElementOperatorFactory;

import java.net.URI;

public interface Neo4jGraphElementFactory extends GraphElementOperatorFactory{
    Neo4jGraphElementOperator withNode(
            Node node
    );
    @Override
    Neo4jGraphElementOperator withUri(URI uri);
}
