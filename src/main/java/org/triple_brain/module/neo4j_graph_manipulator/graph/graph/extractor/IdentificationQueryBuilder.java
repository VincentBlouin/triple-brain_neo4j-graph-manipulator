/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementOperator;

public class IdentificationQueryBuilder {

    public static String identificationReturnQueryPart(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                Neo4jGraphElementOperator.props.identifications.toString()
        );
    }
}
