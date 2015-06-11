/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;
import guru.bubl.module.model.FriendlyResourceFactory;

import java.net.URI;

public interface Neo4jFriendlyResourceFactory extends FriendlyResourceFactory{
    public Neo4jFriendlyResource withNode(Node node);
    @Override
    Neo4jFriendlyResource withUri(URI uri);
}
