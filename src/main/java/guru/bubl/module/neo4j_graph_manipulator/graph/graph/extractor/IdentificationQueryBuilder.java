/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor;

import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.UserGraphNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.IdentificationNeo4j;

public class IdentificationQueryBuilder {

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
                        IdentificationNeo4j.props.external_uri.name()
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
                        IdentificationNeo4j.props.nb_references.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        relationKey,
                        IdentificationNeo4j.props.relation_external_uri.name()
                ) +
                QueryUtils.getLastPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        FriendlyResourceNeo4j.props.creation_date.name()
                ) +
                "]) as " + identificationKey + ", ";
    }
}
