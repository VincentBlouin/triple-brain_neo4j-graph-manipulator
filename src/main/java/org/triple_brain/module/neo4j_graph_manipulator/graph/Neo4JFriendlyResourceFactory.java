package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.FriendlyResourceFactory;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JFriendlyResourceFactory extends FriendlyResourceFactory{
    public Neo4JFriendlyResource createOrLoadFromNode(Node node);
}
