/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex;

import guru.bubl.module.model.graph.vertex.VertexFactory;

import java.net.URI;

public interface VertexFactoryNeo4j extends VertexFactory {
    @Override
    VertexOperatorNeo4j createForOwner(String username);

    @Override
    VertexOperatorNeo4j withUri(URI uri);
}
