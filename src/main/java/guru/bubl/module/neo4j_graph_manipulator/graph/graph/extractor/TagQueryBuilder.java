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
            IDENTIFIER_QUERY_KEY = "id",
            IDENTIFICATION_RELATION_QUERY_KEY = "idr";

    public static String identificationReturnQueryPart(Set<ShareLevel> inShareLevels) {
        return identificationReturnQueryPartUsingKeysForIdentificationRelationAndAlias(
                IDENTIFIER_QUERY_KEY,
                IDENTIFICATION_RELATION_QUERY_KEY,
                inShareLevels
        );
    }

    public static String identificationReturnQueryPartUsingKeysForIdentificationRelationAndAlias(
            String identificationKey,
            String relationKey,
            Set<ShareLevel> inShareLevels
    ) {
        return "COLLECT([" +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        TagOperatorNeo4J.props.external_uri.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        UserGraphNeo4j.URI_PROPERTY_NAME
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        FriendlyResourceNeo4j.props.label.toString()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        FriendlyResourceNeo4j.props.comment.toString()
                ) +
                FriendlyResourceQueryBuilder.imageReturnQueryPart(
                        identificationKey
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        FriendlyResourceNeo4j.props.creation_date.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        "colors"
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        "shareLevel"
                ) +
                (inShareLevels.contains(ShareLevel.PRIVATE) ? QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        ForkOperatorNeo4J.props.nb_private_neighbors.name()
                ) : "null,") +
                (inShareLevels.contains(ShareLevel.FRIENDS) ? QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        ForkOperatorNeo4J.props.nb_friend_neighbors.name()
                ) : "null,") +
                (inShareLevels.contains(ShareLevel.PUBLIC) || inShareLevels.contains(ShareLevel.PUBLIC_WITH_LINK) ? QueryUtils.getLastPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        ForkOperatorNeo4J.props.nb_public_neighbors.name()
                ) : "null") +
                "]) as " + identificationKey + ", ";
    }

    public static String centerTagQueryPart(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                "external_uri"
        );
    }
}
