package org.triple_brain.module.neo4j_graph_manipulator.graph;

/*
 * Copyright Mozilla Public License 1.1
 */
public interface Operation {
    void execute();
    void executeInBatch();
}
