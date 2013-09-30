package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.graph.VertexFactory;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JVertexFactory extends VertexFactory{
    public Neo4JVertexInSubGraph createOrLoadUsingNode(Node node);
}
