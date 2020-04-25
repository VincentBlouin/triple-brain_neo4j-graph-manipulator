/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor;

import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.UserGraphNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.tag.TagOperatorNeo4J;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.fork.ForkOperatorNeo4J;

import java.util.Set;

public class TagQueryBuilder {

    public static final String
            TAG_QUERY_KEY = "id",
            TAG_RELATION_QUERY_KEY = "idr";

    public static String tagReturnQueryPart(Set<ShareLevel> inShareLevels) {
        return tagReturnQueryPartUsingKeysForIdentificationRelationAndAlias(
                TAG_QUERY_KEY,
                TAG_RELATION_QUERY_KEY,
                inShareLevels
        );
    }

    public static String tagReturnQueryPartUsingKeysForIdentificationRelationAndAlias(
            String tagKey,
            String relationKey,
            Set<ShareLevel> inShareLevels
    ) {
        return "COLLECT([" +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        tagKey,
                        TagOperatorNeo4J.props.external_uri.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        tagKey,
                        UserGraphNeo4j.URI_PROPERTY_NAME
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        tagKey,
                        FriendlyResourceNeo4j.props.label.toString()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        tagKey,
                        FriendlyResourceNeo4j.props.comment.toString()
                ) +
                FriendlyResourceQueryBuilder.imageReturnQueryPart(
                        tagKey
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        tagKey,
                        FriendlyResourceNeo4j.props.creation_date.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        tagKey,
                        "colors"
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        tagKey,
                        "shareLevel"
                ) +
                (inShareLevels.contains(ShareLevel.PRIVATE) ? QueryUtils.getPropertyUsingContainerNameQueryPart(
                        tagKey,
                        ForkOperatorNeo4J.props.nb_private_neighbors.name()
                ) : "null,") +
                (inShareLevels.contains(ShareLevel.FRIENDS) ? QueryUtils.getPropertyUsingContainerNameQueryPart(
                        tagKey,
                        ForkOperatorNeo4J.props.nb_friend_neighbors.name()
                ) : "null,") +
                (inShareLevels.contains(ShareLevel.PUBLIC) || inShareLevels.contains(ShareLevel.PUBLIC_WITH_LINK) ? QueryUtils.getLastPropertyUsingContainerNameQueryPart(
                        tagKey,
                        ForkOperatorNeo4J.props.nb_public_neighbors.name()
                ) : "null") +
                "]) as " + tagKey + ", ";
    }

    public static String centerTagQueryPart(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                "external_ui"
        );
    }
}
