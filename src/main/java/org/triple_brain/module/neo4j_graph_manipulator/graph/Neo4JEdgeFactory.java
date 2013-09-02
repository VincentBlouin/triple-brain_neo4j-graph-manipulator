package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.User;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JEdgeFactory {
    public Neo4JEdge loadWithNodeOfOwner(Node node, User owner);
    public Neo4JEdge createWithNodeAndOwner(Node node, User owner);
    public Neo4JEdge createForSourceAndDestinationVertex(
            @Assisted("source") Neo4JVertexInSubGraph sourceVertex,
            @Assisted("destination") Neo4JVertexInSubGraph destinationVertex
    );
    public Neo4JEdge createOrLoadFromUriAndOwner(
            URI uri,
            User owner
    );
}
