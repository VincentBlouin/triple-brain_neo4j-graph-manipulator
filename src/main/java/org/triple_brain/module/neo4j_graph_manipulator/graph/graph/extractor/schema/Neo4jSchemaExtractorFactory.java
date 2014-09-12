/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.schema;

import java.net.URI;

public interface Neo4jSchemaExtractorFactory {
    public Neo4jSchemaExtractor havingUri(URI uri);
}
