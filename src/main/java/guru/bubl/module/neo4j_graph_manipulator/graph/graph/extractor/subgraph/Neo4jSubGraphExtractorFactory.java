/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import java.net.URI;

public interface Neo4jSubGraphExtractorFactory {
    Neo4jSubGraphExtractor withCenterVertexAndDepth(URI centerVertexUri, Integer depth);
}
