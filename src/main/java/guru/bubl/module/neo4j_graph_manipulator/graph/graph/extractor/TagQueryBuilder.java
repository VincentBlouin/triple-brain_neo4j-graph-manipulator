/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor;

import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.UserGraphNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.tag.TagNeo4J;

public class TagQueryBuilder {

    public static final String
            IDENTIFIER_QUERY_KEY = "id",
            IDENTIFICATION_RELATION_QUERY_KEY = "idr";

    public static String identificationReturnQueryPart() {
        return identificationReturnQueryPartUsingKeysForIdentificationRelationAndAlias(
                IDENTIFIER_QUERY_KEY,
                IDENTIFICATION_RELATION_QUERY_KEY
        );
    }

    public static String identificationReturnQueryPartUsingKeysForIdentificationRelationAndAlias(
            String identificationKey,
            String relationKey
    ) {
        return "COLLECT([" +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        TagNeo4J.props.external_uri.name()
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
                        TagNeo4J.props.nb_references.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        relationKey,
                        TagNeo4J.props.relation_external_uri.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        FriendlyResourceNeo4j.props.creation_date.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        "colors"
                ) +
                QueryUtils.getLastPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        "shareLevel"
                ) +
                "]) as " + identificationKey + ", ";
    }

    public static String centerTagQueryPart(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                "external_uri"
        ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        "nb_references"
                );
    }
}
