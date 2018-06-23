/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.image;

import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;

public interface ImageFactoryNeo4j {
    ImagesNeo4j forResource(FriendlyResourceNeo4j friendlyResource);
}
