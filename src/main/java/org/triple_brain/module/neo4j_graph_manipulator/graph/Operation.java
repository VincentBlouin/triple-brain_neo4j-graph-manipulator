/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph;

public interface Operation {
    void execute();
    void executeInBatch();
}
