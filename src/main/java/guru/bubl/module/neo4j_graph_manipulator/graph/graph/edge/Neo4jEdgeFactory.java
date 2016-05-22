/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge;

import com.google.inject.assistedinject.Assisted;
import org.neo4j.graphdb.Node;
import guru.bubl.module.model.graph.vertex.Vertex;

import java.net.URI;

public interface Neo4jEdgeFactory {
    Neo4jEdgeOperator withSourceAndDestinationVertex(
            @Assisted("source") Vertex sourceVertex,
            @Assisted("destination") Vertex destinationVertex
    );
    Neo4jEdgeOperator withUriAndSourceAndDestinationVertex(
            URI uri,
            @Assisted("source") Vertex sourceVertex,
            @Assisted("destination") Vertex destinationVertex
    );
    Neo4jEdgeOperator withUri(
            URI uri
    );
}
