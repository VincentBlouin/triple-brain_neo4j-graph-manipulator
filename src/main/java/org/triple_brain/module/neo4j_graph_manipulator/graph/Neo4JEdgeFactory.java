package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import org.neo4j.graphdb.Node;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JEdgeFactory {

    public Neo4JEdge createOrLoadWithNode(Node node);
    public Neo4JEdge createForSourceAndDestinationVertex(
            @Assisted("source") Neo4JVertexInSubGraph sourceVertex,
            @Assisted("destination") Neo4JVertexInSubGraph destinationVertex
    );
    public Neo4JEdge createOrLoadFromUri(
            URI uri
    );
}
