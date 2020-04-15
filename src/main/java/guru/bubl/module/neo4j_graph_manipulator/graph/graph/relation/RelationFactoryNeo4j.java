/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.relation;

import com.google.inject.assistedinject.Assisted;

import java.net.URI;

public interface RelationFactoryNeo4j {
    RelationOperatorNeo4J withSourceAndDestinationUri(
            @Assisted("source") URI sourceUri,
            @Assisted("destination") URI destinationUri
    );
    RelationOperatorNeo4J withUriAndSourceAndDestinationVertex(
            URI uri,
            @Assisted("source") URI sourceUri,
            @Assisted("destination") URI destinationUri
    );
    RelationOperatorNeo4J withUri(
            URI uri
    );
}
