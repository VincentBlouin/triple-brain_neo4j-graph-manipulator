/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor;

import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.Neo4jImages;

public class FriendlyResourceQueryBuilder {

    public static String returnQueryPartUsingPrefix(String prefix) {
        return
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        Neo4jUserGraph.URI_PROPERTY_NAME
                ) +
                        QueryUtils.getPropertyUsingContainerNameQueryPart(
                                prefix,
                                Neo4jFriendlyResource.props.label.toString()
                        ) +
                        QueryUtils.getPropertyUsingContainerNameQueryPart(
                                prefix,
                                Neo4jFriendlyResource.props.comment.toString()
                        ) +
                        QueryUtils.getPropertyUsingContainerNameQueryPart(
                                prefix,
                                Neo4jFriendlyResource.props.creation_date.name()
                        ) +
                        QueryUtils.getPropertyUsingContainerNameQueryPart(
                                prefix,
                                Neo4jFriendlyResource.props.last_modification_date.name()
                        );
    }

    public static String imageReturnQueryPart(String key) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                key,
                Neo4jImages.props.images.name()
        );
    }
}
