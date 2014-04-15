package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.vertex;

import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.graph.vertex.VertexFactory;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4jVertexFactory extends VertexFactory{
    Neo4jVertexInSubGraphOperator createOrLoadUsingNode(Node node);
    @Override
    Neo4jVertexInSubGraphOperator createForOwnerUsername(String username);
    @Override
    Neo4jVertexInSubGraphOperator withUri(URI uri);
}
