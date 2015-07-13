///*
// * Copyright Vincent Blouin under the GPL License version 3
// */
//
//package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.identification;
//
//
//import org.junit.Ignore;
//import org.junit.Test;
//import org.neo4j.graphdb.Node;
//import org.neo4j.rest.graphdb.util.QueryResult;
//import guru.bubl.module.model.UserUris;
//import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
//import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
//import guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.AdminOperationsOnDatabase;
//
//import java.util.Iterator;
//import java.util.Map;
//
//import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;
//@Ignore
//public class ConvertIdentifications4 extends AdminOperationsOnDatabase{
//
//    @Test
//    public void go(){
//        QueryResult<Map<String, Object>> results = queryEngine.query(
//                "START relation=node(*) " +
//                        "MATCH relation-[:" +
//                        Relationships.SOURCE_VERTEX +
//                        "]->vertex " +
//                        "RETURN relation",
//                map()
//        );
//        Iterator<Map<String,Object>> it = results.iterator();
//        while(it.hasNext()){
//            Map<String, Object> result = it.next();
//            Node node = (Node) result.get("relation");
//            if(node.hasProperty("external_uri")){
//                convert(node);
//            }
//        }
//    }
//
//    public void convert(Node node){
//        boolean isIdentifiedToGraphElement = node.getProperty("external_uri").toString().startsWith("/service");
//        if(!isIdentifiedToGraphElement) {
//            return;
//        }
//        String owner = node.getProperty("owner").toString();
//        String uri = new UserUris(owner).generateEdgeUri().toString();
//        System.out.println(owner);
//        System.out.println(node.getProperty(Neo4jFriendlyResource.props.label.toString()));
//        System.out.println(node.getProperty("uri"));
//        System.out.println(node.getProperty("external_uri"));
//        node.setProperty("uri", uri);
//        node.removeProperty("external_uri");
//    }
//}
