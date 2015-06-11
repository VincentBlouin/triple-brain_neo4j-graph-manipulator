/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.schema;

import guru.bubl.module.model.graph.schema.SchemaOperator;

import java.net.URI;

public interface SchemaFactory {
    SchemaOperator createForOwnerUsername(String username);
    SchemaOperator withUri(URI uri);
}
