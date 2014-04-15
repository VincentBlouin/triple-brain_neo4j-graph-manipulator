package org.triple_brain.module.neo4j_graph_manipulator.graph.image;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.batch.BatchCallback;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Relationships;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class Neo4jImages {

    public enum props {
        url_for_small,
        url_for_bigger
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

    public void addAll(final Set<Image> images){
        restApi.executeBatch(new BatchCallback<Object>() {
            @Override
            public Object recordBatch(RestAPI restAPI) {
                for(Image image : images){
                    addImage(
                            image
                    );
                }
                return null;
            }
        });
    }

    public Set<Image> get(){
        QueryResult<Map<String, Object>> results = queryEngine.query(
                friendlyResource.queryPrefix() +
                        "MATCH n-[:"+ Relationships.HAS_IMAGE+"]->image " +
                        "RETURN " +
                        "image." + props.url_for_small + " as uri_for_small, " +
                        "image." + props.url_for_bigger + " as uri_for_bigger ",
                map()
        );
        Set<Image> images = new HashSet<Image>();
        for(Map<String,Object> result : results){
            images.add(
                    Image.withUriForSmallAndBigger(
                            URI.create(
                                    result.get(
                                            "uri_for_small"
                                    ).toString()
                            ),
                            URI.create(
                                    result.get(
                                            "uri_for_bigger"
                                    ).toString()
                            )
                    )
            );
        }
        return images;
    }

    private void addImage(Image image) {
        queryEngine.query(
                friendlyResource.queryPrefix() +
                        "MERGE (image {" +
                        props.url_for_small + ": {uri_for_small}, " +
                        props.url_for_bigger + ": {uri_for_bigger} " +
                        "}) " +
                        "ON CREATE SET " +
                        "image.uri={image_uri}" +
                        "CREATE UNIQUE " +
                        "n-[:" + Relationships.HAS_IMAGE + "]->image ",
                map(
                        "uri_for_small", image.urlForSmall().toString(),
                        "uri_for_bigger", image.urlForBigger().toString(),
                        "image_uri", "/image/" + UUID.randomUUID().toString()
                )
        );
    }

}
