/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import com.google.gson.reflect.TypeToken;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.json.JsonUtils;
import guru.bubl.module.model.search.GraphElementSearchResult;
import org.neo4j.driver.v1.Record;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface SearchResultBuilder {
    GraphElementSearchResult build();

    default Map<URI, String> getContext() {
        if (getRow().get("context").asObject() == null) {
            return new HashMap<>();
        }
        String contextStr = getRow().get("context").asString();
        return JsonUtils.getGson().fromJson(
                contextStr,
                new TypeToken<Map<URI, String>>() {
                }.getType()
        );
    }

    default ShareLevel extractShareLevel() {
        if (getRow().get("shareLevel").asObject() == null) {
            return ShareLevel.PRIVATE;
        }
        Integer shareLevel = getRow().get("shareLevel").asInt();
        return ShareLevel.get(shareLevel);
    }

    default Integer getNbVisits() {
        return getRow().get("nbVisits").asObject() == null ?
                0 : getRow().get("nbVisits").asInt();
    }

    Record getRow();
}
