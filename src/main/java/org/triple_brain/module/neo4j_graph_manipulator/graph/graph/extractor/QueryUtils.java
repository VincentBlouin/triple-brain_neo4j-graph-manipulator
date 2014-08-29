package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

/*
* Copyright Mozilla Public License 1.1
*/
public class QueryUtils {
    public static String getPropertyUsingContainerNameQueryPart(String containerName, String propertyName) {
        return containerName + "." + propertyName + ", ";
    }
}
