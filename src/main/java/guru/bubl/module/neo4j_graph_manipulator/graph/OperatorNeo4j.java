/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import guru.bubl.module.model.UserUris;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.UserGraphNeo4j;

import java.net.URI;
import java.util.Date;
import java.util.Map;

public interface OperatorNeo4j {
    default String queryPrefix() {
        return String.format(
                "MATCH%s ",
                addToSelectUsingVariableName(
                        "n",
                        "uri"
                )
        );
    }

    URI uri();

    default String addToSelectUsingVariableName(String variableName, String uriName) {
        return String.format(
                "(%s:Resource{uri:$%s}) ",
                variableName,
                uriName
        );
    }

    default Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Long now = new Date().getTime();
        Map<String, Object> newMap = RestApiUtilsNeo4j.map(
                UserGraphNeo4j.URI_PROPERTY_NAME, uri().toString(),
                FriendlyResourceNeo4j.props.owner.name(), UserUris.ownerUserNameFromUri(uri()),
                FriendlyResourceNeo4j.props.creation_date.name(), now,
                FriendlyResourceNeo4j.props.last_modification_date.name(), now
        );
        newMap.putAll(
                map
        );
        return newMap;
    }
}
