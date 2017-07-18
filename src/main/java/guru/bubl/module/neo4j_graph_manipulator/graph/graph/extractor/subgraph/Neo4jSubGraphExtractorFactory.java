/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import com.google.inject.assistedinject.Assisted;

import java.net.URI;

public interface Neo4jSubGraphExtractorFactory {
    Neo4jSubGraphExtractor withCenterVertexAndDepth(
            URI centerVertexUri,
            @Assisted("depth") Integer depth
    );
    Neo4jSubGraphExtractor withCenterVertexDepthAndResultsLimit(
            URI centerVertexUri,
            @Assisted("depth") Integer depth,
            @Assisted("resultsLimit") Integer resultsLimit
    );
}
