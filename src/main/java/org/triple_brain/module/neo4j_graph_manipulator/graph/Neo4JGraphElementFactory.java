package org.triple_brain.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.Node;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4JGraphElementFactory {
    Neo4JGraphElement withNode(
            Node node
    );
}
