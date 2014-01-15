package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import org.neo4j.graphdb.Node;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JEdgeFactory {

    public Neo4JEdgeOperator createOrLoadWithNode(Node node);
    public Neo4JEdgeOperator createForSourceAndDestinationVertex(
            @Assisted("source") Neo4JVertexInSubGraphOperator sourceVertexOperator,
            @Assisted("destination") Neo4JVertexInSubGraphOperator destinationVertexOperator
    );
    public Neo4JEdgeOperator createOrLoadFromUri(
            URI uri
    );
}
