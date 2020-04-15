/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge;

import com.google.inject.assistedinject.Assisted;

import java.net.URI;

public interface EdgeFactoryNeo4j {
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
