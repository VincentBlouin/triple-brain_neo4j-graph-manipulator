/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import com.google.gson.reflect.TypeToken;
import guru.bubl.module.model.json.JsonUtils;
import guru.bubl.module.model.search.GraphElementSearchResult;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public interface SearchResultBuilder {
    GraphElementSearchResult build();

    default Map<URI, String> getContext() throws SQLException {
        String contextStr = getRow().getString("context");
        if(null == contextStr){
            return new HashMap<>();
        }
        return JsonUtils.getGson().fromJson(
                contextStr,
                new TypeToken<Map<URI, String>>() {
                }.getType()
        );
    }

    ResultSet getRow();
}
