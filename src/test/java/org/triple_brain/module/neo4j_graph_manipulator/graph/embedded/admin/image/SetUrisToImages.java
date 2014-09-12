/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin.image;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin.AdminOperationsOnDatabase;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

@Ignore
public class SetUrisToImages extends AdminOperationsOnDatabase {

    @Test
    public void go(){
        QueryResult<Map<String, Object>> results = queryEngine.query(
                "start n=node(*) MATCH n-[:HAS_IMAGE]->image return image",
                map()
        );
        Iterator<Map<String,Object>> it = results.iterator();
        while(it.hasNext()){
            Map<String, Object> result = it.next();
            Node node = (Node) result.get("image");
            node.setProperty("image_uri", "/image/" + UUID.randomUUID().toString());
        }
    }
}
