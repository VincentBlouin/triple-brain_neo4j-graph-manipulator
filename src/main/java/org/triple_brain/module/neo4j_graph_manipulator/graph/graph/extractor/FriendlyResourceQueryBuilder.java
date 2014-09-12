/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

import com.hp.hpl.jena.vocabulary.RDFS;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import org.triple_brain.module.neo4j_graph_manipulator.graph.image.Neo4jImages;

public class FriendlyResourceQueryBuilder {

    public static String returnQueryPartUsingPrefix(String prefix) {
        return
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        Neo4jUserGraph.URI_PROPERTY_NAME
                ) +
                        QueryUtils.getPropertyUsingContainerNameQueryPart(
                                prefix,
                                "`" + RDFS.label.getURI() + "`"
                        ) +
                        QueryUtils.getPropertyUsingContainerNameQueryPart(
                                prefix,
                                "`" + RDFS.comment.getURI() + "`"
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

    public static String imageReturnQueryPart(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                Neo4jImages.props.images.name()
        );
    }
}
