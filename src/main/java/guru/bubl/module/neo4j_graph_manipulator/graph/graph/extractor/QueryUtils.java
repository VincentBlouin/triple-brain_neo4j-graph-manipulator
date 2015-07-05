/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor;

public class QueryUtils {
    public static String getLastPropertyUsingContainerNameQueryPart(String containerName, String propertyName) {
        return getLastOrNotPropertyUsingContainerNameQueryPart(
                true,
                containerName,
                propertyName
        );
    }
    public static String getPropertyUsingContainerNameQueryPart(String containerName, String propertyName) {
        return getLastOrNotPropertyUsingContainerNameQueryPart(
                false,
                containerName,
                propertyName
        );
    }

    private static String getLastOrNotPropertyUsingContainerNameQueryPart(Boolean isLast, String containerName, String propertyName) {
        String queryPart = containerName + "." + propertyName;
        if(!isLast){
            queryPart += ", ";
        }
        return queryPart;
    }
}
