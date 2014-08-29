package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4jSubGraphExtractorFactory {
    public Neo4jSubGraphExtractor withCenterVertexAndDepth(URI centerVertexUri, Integer depth);
}
