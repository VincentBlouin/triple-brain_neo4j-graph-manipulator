/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import org.neo4j.graphdb.Node;
import guru.bubl.module.model.graph.GraphElementOperatorFactory;

import java.net.URI;

public interface GraphElementFactoryNeo4j extends GraphElementOperatorFactory{
    GraphElementOperatorNeo4j withNode(
            Node node
    );
    @Override
    GraphElementOperatorNeo4j withUri(URI uri);
}
