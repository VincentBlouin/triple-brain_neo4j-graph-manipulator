/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.graph.IdentificationFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jIdentification;

import java.net.URI;

public interface Neo4jIdentificationFactory extends IdentificationFactory {
    @Override
    Neo4jIdentification withUri(URI uri);
    public Neo4jIdentification withNode(Node node);
}
