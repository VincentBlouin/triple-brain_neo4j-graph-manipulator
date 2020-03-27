/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import com.google.gson.reflect.TypeToken;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.vertex.NbNeighbors;
import guru.bubl.module.model.graph.vertex.NbNeighborsPojo;
import guru.bubl.module.model.json.JsonUtils;
import guru.bubl.module.model.search.GraphElementSearchResult;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Value;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface SearchResultBuilder {
    GraphElementSearchResult build();

    default String getContext() {
        Value value = getRow().get("context");
        return value.asObject() == null ? "" : value.asString();
    }

    default ShareLevel extractShareLevel() {
        if (getRow().get("shareLevel").asObject() == null) {
            return ShareLevel.PRIVATE;
        }
        Integer shareLevel = getRow().get("shareLevel").asInt();
        return ShareLevel.get(shareLevel);
    }

    default Integer getNbVisits() {
        return getRow().get("n.nb_visits").asObject() == null ?
                0 : getRow().get("n.nb_visits").asInt();
    }

    default NbNeighborsPojo buildNbNeighbors() {
        NbNeighborsPojo nbNeighborsPojo = new NbNeighborsPojo();
        if (getInShareLevels().contains(ShareLevel.PRIVATE)) {
            nbNeighborsPojo.setPrivate(getRow().get("n.nb_private_neighbors").asInt());
        }
        if (getInShareLevels().contains(ShareLevel.FRIENDS)) {
            nbNeighborsPojo.setFriend(getRow().get(
                    "n.nb_friend_neighbors"
            ).asInt());
        }
        nbNeighborsPojo.setPublic(getRow().get(
                "n.nb_public_neighbors"
        ).asInt());
        return nbNeighborsPojo;
    }

    Record getRow();

    default Set<ShareLevel> getInShareLevels() {
        return null;
    }
}
