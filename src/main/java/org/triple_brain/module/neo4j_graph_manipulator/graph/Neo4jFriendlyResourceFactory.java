/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.FriendlyResourceFactory;

import java.net.URI;

public interface Neo4jFriendlyResourceFactory extends FriendlyResourceFactory{
    public Neo4jFriendlyResource withNode(Node node);
    @Override
    Neo4jFriendlyResource withUri(URI uri);
}
