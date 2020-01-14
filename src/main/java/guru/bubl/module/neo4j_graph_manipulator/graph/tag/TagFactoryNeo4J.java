/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.tag;

import guru.bubl.module.model.graph.tag.TagFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.tag.TagNeo4J;

import java.net.URI;

public interface TagFactoryNeo4J extends TagFactory {
    @Override
    TagNeo4J withUri(URI uri);
}
