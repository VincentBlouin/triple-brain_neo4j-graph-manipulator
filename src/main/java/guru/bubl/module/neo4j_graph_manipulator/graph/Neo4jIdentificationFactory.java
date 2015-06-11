/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jIdentification;
import org.neo4j.graphdb.Node;
import guru.bubl.module.model.graph.IdentificationFactory;

import java.net.URI;

public interface Neo4jIdentificationFactory extends IdentificationFactory {
    @Override
    Neo4jIdentification withUri(URI uri);
    public Neo4jIdentification withNode(Node node);
}
