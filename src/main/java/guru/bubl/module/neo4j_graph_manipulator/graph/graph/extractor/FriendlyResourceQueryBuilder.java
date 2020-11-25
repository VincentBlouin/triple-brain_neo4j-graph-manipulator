/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor;

import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.UserGraphNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.ImagesNeo4j;

public class FriendlyResourceQueryBuilder {

    public static String returnQueryPartUsingPrefix(String prefix) {
        return
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        UserGraphNeo4j.URI_PROPERTY_NAME
                ) +
                        QueryUtils.getPropertyUsingContainerNameQueryPart(
                                prefix,
                                FriendlyResourceNeo4j.props.label.toString()
                        ) +
                        QueryUtils.getPropertyUsingContainerNameQueryPart(
                                prefix,
                                FriendlyResourceNeo4j.props.comment.toString()
                        ) +
                        QueryUtils.getPropertyUsingContainerNameQueryPart(
                                prefix,
                                FriendlyResourceNeo4j.props.creation_date.name()
                        ) +
                        QueryUtils.getPropertyUsingContainerNameQueryPart(
                                prefix,
                                "copied_from_uri"
                        ) +
                        QueryUtils.getPropertyUsingContainerNameQueryPart(
                                prefix,
                                FriendlyResourceNeo4j.props.last_modification_date.name()
                        );
    }

    public static String imageReturnQueryPart(String key) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                key,
                ImagesNeo4j.props.images.name()
        );
    }
}
