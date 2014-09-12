/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jIdentification;

public class IdentificationQueryBuilder {
    public static String typeReturnQueryPart(String prefix) {
        return identificationReturnQueryPart(
                prefix + "_type"
        );
    }

    public static String sameAsReturnQueryPart(String prefix) {
        return identificationReturnQueryPart(
                prefix + "_same_as"
        );
    }

    public static String genericIdentificationReturnQueryPart(String prefix) {
        return identificationReturnQueryPart(
                prefix + "_generic_identification"
        );
    }


    private static String identificationReturnQueryPart(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix, Neo4jIdentification.props.external_uri.name()
        ) +
                FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix(
                        prefix
                ) + FriendlyResourceQueryBuilder.imageReturnQueryPart(prefix);
    }
}
