package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import org.neo4j.graphdb.Node;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4jEdgeFactory {

    public Neo4jEdgeOperator createOrLoadWithNode(Node node);
    public Neo4jEdgeOperator createForSourceAndDestinationVertex(
            @Assisted("source") Neo4jVertexInSubGraphOperator sourceVertexOperator,
            @Assisted("destination") Neo4jVertexInSubGraphOperator destinationVertexOperator
    );
    public Neo4jEdgeOperator createOrLoadFromUri(
            URI uri
    );
}
