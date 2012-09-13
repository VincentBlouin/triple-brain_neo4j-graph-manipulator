package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.User;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JVertexFactory  {
    public Neo4JVertex loadUsingNodeOfOwner(Node node, User owner);
    public Neo4JVertex createUsingEmptyNodeUriAndOwner(Node node, URI uri, User owner);
}
