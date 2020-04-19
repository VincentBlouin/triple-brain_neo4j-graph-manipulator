/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element;

import guru.bubl.module.model.graph.graph_element.GraphElementOperatorFactory;

import java.net.URI;

public interface GraphElementFactoryNeo4j extends GraphElementOperatorFactory{
    @Override
    GraphElementOperatorNeo4j withUri(URI uri);
}
