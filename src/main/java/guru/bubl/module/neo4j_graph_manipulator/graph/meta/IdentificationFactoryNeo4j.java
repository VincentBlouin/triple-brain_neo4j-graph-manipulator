/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.meta;

import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.IdentificationNeo4j;
import org.neo4j.graphdb.Node;
import guru.bubl.module.model.graph.identification.IdentificationFactory;

import java.net.URI;

public interface IdentificationFactoryNeo4j extends IdentificationFactory {
    @Override
    IdentificationNeo4j withUri(URI uri);
    IdentificationNeo4j withNode(Node node);
}
