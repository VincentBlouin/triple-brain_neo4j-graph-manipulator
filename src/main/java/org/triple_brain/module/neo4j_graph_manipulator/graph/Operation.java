/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph;

public interface Operation {
    void execute();
    void executeInBatch();
}
