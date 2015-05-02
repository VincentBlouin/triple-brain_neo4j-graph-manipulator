/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

public class QueryUtils {
    public static String getPropertyUsingContainerNameQueryPart(String containerName, String propertyName) {
        return containerName + "." + propertyName + ", ";
    }
}
