/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin.image;

import org.apache.commons.codec.binary.Base64;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.common_utils.DataFetcher;
import org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin.AdminOperationsOnDatabase;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

@Ignore
public class SetSmallImagesToBase64 extends AdminOperationsOnDatabase {

    @Test
    public void asdf(){
        String t = "/service/users/vince/graph/vertex/3244864f-68df-46dd-9962-159220643c4e/image/34847527-9cf9-4019-beec-3d7f484ffad3/small";
        System.out.println(urlOfUserUpdatedImage(t));
    }

    @Test
    public void go(){
        QueryResult<Map<String, Object>> results = queryEngine.query(
                "start n=node(*) MATCH n-[:HAS_IMAGE]->image return image, image.url_for_small as url_for_small",
                map()
        );
        Iterator<Map<String,Object>> it = results.iterator();
        while(it.hasNext()){
            Map<String, Object> result = it.next();
            Node node = (Node) result.get("image");
            String urlStr = result.get("url_for_small").toString();
            try {
                URL url = new URL(
                        urlStr.startsWith("/service") ?
                                urlOfUserUpdatedImage(urlStr) : urlStr
                );
                String base64 = Base64.encodeBase64String(
                        DataFetcher.downloadImageAtUrl(url)
                );
                node.setProperty("base64_for_small", base64);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private String urlOfUserUpdatedImage(String urlStr){
        Integer start = urlStr.indexOf("image/");
        return "http://localhost/image/" + urlStr.substring(start + 6).replace("/", "_");
    }
}
