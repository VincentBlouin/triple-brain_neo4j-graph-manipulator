/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge;

import com.google.inject.assistedinject.Assisted;
import guru.bubl.module.model.graph.vertex.Vertex;

import java.net.URI;

public interface EdgeFactoryNeo4j {
    EdgeOperatorNeo4j withSourceAndDestinationUri(
            @Assisted("source") URI sourceUri,
            @Assisted("destination") URI destinationUri
    );
    EdgeOperatorNeo4j withUriAndSourceAndDestinationVertex(
            URI uri,
            @Assisted("source") URI sourceUri,
            @Assisted("destination") URI destinationUri
    );
    EdgeOperatorNeo4j withUri(
            URI uri
    );
}
