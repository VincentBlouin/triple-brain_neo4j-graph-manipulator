package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.FriendlyResourceFactory;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4jFriendlyResourceFactory extends FriendlyResourceFactory{
    public Neo4jFriendlyResource withNode(Node node);
    @Override
    Neo4jFriendlyResource withUri(URI uri);
}
