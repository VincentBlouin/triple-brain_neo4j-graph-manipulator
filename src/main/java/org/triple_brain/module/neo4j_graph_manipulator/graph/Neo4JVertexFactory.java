package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JVertexFactory  {
    public Neo4JVertexInSubGraph createOrLoadUsingNode(Node node);
    public Neo4JVertexInSubGraph createOrLoadUsingUri(URI uri);
    public Neo4JVertexInSubGraph createForOwnerUsername(String username);
}
