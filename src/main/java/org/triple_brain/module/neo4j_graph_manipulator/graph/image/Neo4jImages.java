package org.triple_brain.module.neo4j_graph_manipulator.graph.image;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.json.ImageJson;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class Neo4jImages {

    public enum props {
        images
    }

    protected RestAPI restApi;
    protected QueryEngine<Map<String,Object>> queryEngine;
    protected Neo4jFriendlyResource friendlyResource;

    @AssistedInject
    public Neo4jImages(
            RestAPI restApi,
            QueryEngine queryEngine,
            @Assisted Neo4jFriendlyResource friendlyResource
    ) {
        this.restApi = restApi;
        this.queryEngine = queryEngine;
        this.friendlyResource = friendlyResource;
    }

    public void addAll(Set<Image> images){
        Set<Image> current = get();
        current.addAll(images);
        queryEngine.query(
                friendlyResource.queryPrefix() +
                        "SET n." + props.images + "= { " + props.images + "} ",
                map(
                        props.images.name(), ImageJson.toJsonArray(current).toString()
                )
        );
    }

    public Set<Image> get(){
        QueryResult<Map<String, Object>> result = queryEngine.query(
                friendlyResource.queryPrefix() +
                        "return n." + props.images + " as images",
                map()
        );
        if(!result.iterator().hasNext()){
            return new HashSet<>();
        }
        Object imagesValue = result.iterator().next().get("images");
        if(imagesValue == null){
            return new HashSet<>();
        }
        return ImageJson.fromJson(
                imagesValue.toString()
        );
    }

}
