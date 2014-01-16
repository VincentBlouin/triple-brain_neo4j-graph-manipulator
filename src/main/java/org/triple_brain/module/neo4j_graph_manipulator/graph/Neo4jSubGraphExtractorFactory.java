package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.triple_brain.module.model.graph.vertex.Vertex;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4jSubGraphExtractorFactory {
    public Neo4jSubGraphExtractor withCenterVertexAndDepth(Vertex centerVertex, Integer depth);
}
