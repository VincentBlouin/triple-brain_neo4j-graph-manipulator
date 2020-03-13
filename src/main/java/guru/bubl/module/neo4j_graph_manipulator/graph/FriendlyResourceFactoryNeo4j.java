/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import guru.bubl.module.model.FriendlyResourceFactory;

import java.net.URI;

public interface FriendlyResourceFactoryNeo4j extends FriendlyResourceFactory{
    @Override
    FriendlyResourceNeo4j withUri(URI uri);
}
