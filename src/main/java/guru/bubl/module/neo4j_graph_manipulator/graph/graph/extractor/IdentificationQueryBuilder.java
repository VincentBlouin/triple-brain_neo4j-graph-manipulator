/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor;

import guru.bubl.module.model.FriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jIdentification;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.Neo4jSubGraphExtractor;

public class IdentificationQueryBuilder {

    public static final String
            IDENTIFICATION_QUERY_KEY = "id",
            IDENTIFICATION_RELATION_QUERY_KEY = "idr";

    public static String identificationReturnQueryPart() {
        return "COLLECT([" +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        IDENTIFICATION_QUERY_KEY,
                        Neo4jIdentification.props.external_uri.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        IDENTIFICATION_QUERY_KEY,
                        Neo4jUserGraph.URI_PROPERTY_NAME
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        IDENTIFICATION_QUERY_KEY,
                        Neo4jFriendlyResource.props.label.toString()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        IDENTIFICATION_QUERY_KEY,
                        Neo4jFriendlyResource.props.comment.toString()
                ) +
                FriendlyResourceQueryBuilder.imageReturnQueryPart(
                        IDENTIFICATION_QUERY_KEY
                ) +
                QueryUtils.getLastPropertyUsingContainerNameQueryPart(
                        IDENTIFICATION_RELATION_QUERY_KEY,
                        "type"
                ) +
                "]) as " + IDENTIFICATION_QUERY_KEY + ", ";
    }
}
