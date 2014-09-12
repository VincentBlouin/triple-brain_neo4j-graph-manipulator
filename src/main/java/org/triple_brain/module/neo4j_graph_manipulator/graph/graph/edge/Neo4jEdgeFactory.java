/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.edge;

import com.google.inject.assistedinject.Assisted;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.graph.vertex.Vertex;

import java.net.URI;

public interface Neo4jEdgeFactory {

    public Neo4jEdgeOperator createOrLoadWithNode(Node node);
    public Neo4jEdgeOperator withSourceAndDestinationVertex(
            @Assisted("source") Vertex sourceVertex,
            @Assisted("destination") Vertex destinationVertex
    );
    public Neo4jEdgeOperator withUri(
            URI uri
    );
}
