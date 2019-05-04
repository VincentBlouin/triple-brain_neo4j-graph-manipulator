///*
// * Copyright Vincent Blouin under the GPL License version 3
// */
//
//package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.image;
//
//import com.google.inject.Guice;
//import com.google.inject.Inject;
//import com.google.inject.Injector;
//import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;
//import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jModule;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.neo4j.graphdb.Node;
//import org.neo4j.rest.graphdb.util.QueryResult;
//import guru.bubl.module.model.Image;
//import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
//import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
//import guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.AdminOperationsOnDatabase;
//
//import java.net.URI;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Set;
//
//import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;
//
//@Ignore
//public class ConvertImagesToJson extends AdminOperationsOnDatabase{
//
//    @Inject
//    Neo4jFriendlyResourceFactory friendlyResourceFactory;
//
//    @Test
//    public void go() {
//        Injector injector = Guice.createInjector(
//                Neo4jModule.forTestingUsingRest()
//        );
//        injector.injectMembers(this);
//        QueryResult<Map<String, Object>> results = queryEngine.query(
//                "start n=node(*) MATCH n-[:HAS_IMAGE]->image return n",
//                map()
//        );
//        Iterator<Map<String,Object>> it = results.iterator();
//        while(it.hasNext()){
//            Map<String, Object> result = it.next();
//            Node node = (Node) result.get("n");
//            Neo4jFriendlyResource friendlyResource = friendlyResourceFactory.withNode(node);
//            Set<Image> images = get(friendlyResource);
//            friendlyResource.addImages(images);
//        }
//    }
//
//
//    private Set<Image> get(Neo4jFriendlyResource friendlyResource) {
//        QueryResult<Map<String, Object>> results = queryEngine.query(
//                friendlyResource.queryPrefix() +
//                        "MATCH n-[:" + Relationships.HAS_IMAGE + "]->image " +
//                        "RETURN " +
//                        "image.base64_for_small as base_64_for_small, " +
//                        "image.url_for_bigger as uri_for_bigger ",
//                map()
//        );
//        Set<Image> images = new HashSet<>();
//        for (Map<String, Object> result : results) {
//            images.add(
//                    Image.withUrlForSmallAndUriForBigger(
//                            result.get(
//                                    "base_64_for_small"
//                            ).toString(),
//                            URI.create(
//                                    result.get(
//                                            "uri_for_bigger"
//                                    ).toString()
//                            )
//                    )
//            );
//        }
//        return images;
//    }
//
//}
