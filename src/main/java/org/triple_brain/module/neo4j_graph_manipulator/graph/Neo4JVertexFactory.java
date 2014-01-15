package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.graph.vertex.VertexFactory;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JVertexFactory extends VertexFactory{
    Neo4JVertexInSubGraphOperator createOrLoadUsingNode(Node node);
    @Override
    Neo4JVertexInSubGraphOperator createForOwnerUsername(String username);
}
