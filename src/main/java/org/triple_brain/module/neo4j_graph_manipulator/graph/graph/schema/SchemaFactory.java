package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.schema;

import org.triple_brain.module.model.graph.schema.SchemaOperator;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface SchemaFactory {
    SchemaOperator createForOwnerUsername(String username);
    SchemaOperator withUri(URI uri);
}
