/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.schema;

import java.net.URI;

public interface Neo4jSchemaExtractorFactory {
    public Neo4jSchemaExtractor havingUri(URI uri);
}
