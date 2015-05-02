/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin.identification;


import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin.AdminOperationsOnDatabase;

import java.util.Iterator;
import java.util.Map;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

@Ignore
public class ConvertIdentifications3 extends AdminOperationsOnDatabase{

    @Test
    public void go(){
        QueryResult<Map<String, Object>> results = queryEngine.query(
                "START n=node(*) match (n:vertex) return n",
                map()
        );
        Iterator<Map<String,Object>> it = results.iterator();
        while(it.hasNext()){
            Map<String, Object> result = it.next();
            Node node = (Node) result.get("n");
            if(node.hasProperty("external_uri")){
                convert(node);
            }
        }
    }

    public void convert(Node node){
        boolean isIdentifiedToGraphElement = node.getProperty("external_uri").toString().startsWith("/service");
        if(!isIdentifiedToGraphElement) {
            return;
        }
        String owner = node.getProperty("owner").toString();
        String vertexUri =new UserUris(owner).generateVertexUri().toString();
        node.setProperty("uri", vertexUri);
        node.removeProperty("external_uri");
    }
}
