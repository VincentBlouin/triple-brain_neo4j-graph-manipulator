package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.schema;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4jSchemaExtractorFactory {
    public Neo4jSchemaExtractor havingUri(URI uri);
}
