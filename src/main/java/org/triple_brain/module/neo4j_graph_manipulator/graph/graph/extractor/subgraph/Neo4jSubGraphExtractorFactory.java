/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import java.net.URI;

public interface Neo4jSubGraphExtractorFactory {
    public Neo4jSubGraphExtractor withCenterVertexAndDepth(URI centerVertexUri, Integer depth);
}
