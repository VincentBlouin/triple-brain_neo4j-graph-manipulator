/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge;

import com.google.inject.assistedinject.Assisted;
import guru.bubl.module.model.graph.vertex.Vertex;

import java.net.URI;

public interface EdgeFactoryNeo4j {
    EdgeOperatorNeo4j withSourceAndDestinationVertex(
            @Assisted("source") Vertex sourceVertex,
            @Assisted("destination") Vertex destinationVertex
    );
    EdgeOperatorNeo4j withUriAndSourceAndDestinationVertex(
            URI uri,
            @Assisted("source") Vertex sourceVertex,
            @Assisted("destination") Vertex destinationVertex
    );
    EdgeOperatorNeo4j withUri(
            URI uri
    );
}
