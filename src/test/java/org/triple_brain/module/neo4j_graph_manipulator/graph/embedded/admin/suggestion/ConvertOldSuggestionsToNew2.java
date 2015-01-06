/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin.suggestion;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.triple_brain.module.model.suggestion.SuggestionPojo;

import java.util.Set;

public class ConvertOldSuggestionsToNew2 {
    private static Gson gson = new Gson();

    public static JSONArray multipleToJson(Set<SuggestionPojo> suggestionPojo) {
        try {
            return new JSONArray(gson.toJson(
                    suggestionPojo
            ));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<SuggestionPojo> fromJsonArray(String json) {
        return gson.fromJson(
                json,
                new TypeToken<Set<SuggestionPojo>>() {
                }.getType()
        );
    }
}
