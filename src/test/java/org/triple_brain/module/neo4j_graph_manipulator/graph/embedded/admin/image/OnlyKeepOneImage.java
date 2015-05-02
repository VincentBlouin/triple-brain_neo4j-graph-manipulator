/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin.image;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.json.ImageJson;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jModule;
import org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin.AdminOperationsOnDatabase;
import org.triple_brain.module.neo4j_graph_manipulator.graph.image.Neo4jImages;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

@Ignore
public class OnlyKeepOneImage extends AdminOperationsOnDatabase {
    @Inject
    Neo4jFriendlyResourceFactory friendlyResourceFactory;

    @Test
    public void go() {
        Injector injector = Guice.createInjector(
                Neo4jModule.forTestingUsingRest()
        );
        injector.injectMembers(this);
        QueryResult<Map<String, Object>> results = queryEngine.query(
                "START n = node(*) where n.images is not null and n.images <> \"[]\" RETURN n",
                map()
        );
        Iterator<Map<String,Object>> it = results.iterator();
        while(it.hasNext()){
            Map<String, Object> result = it.next();
            Node node = (Node) result.get("n");
            Neo4jFriendlyResource friendlyResource = friendlyResourceFactory.withNode(node);
            Set<Image> images = friendlyResource.images();
            update(
                    friendlyResource,
                    images
            );
        }
    }

    private void update(Neo4jFriendlyResource friendlyResource, Set<Image> images){
        if(images.size() < 2){
            return;
        }
        Set<Image> onlyOne = new HashSet<>();
        onlyOne.add(images.iterator().next());
        queryEngine.query(
                friendlyResource.queryPrefix() +
                        "SET n." + Neo4jImages.props.images + "= { " + Neo4jImages.props.images + "} ",
                map(
                        Neo4jImages.props.images.name(), ImageJson.toJsonArray(onlyOne).toString()
                )
        );
    }
}
