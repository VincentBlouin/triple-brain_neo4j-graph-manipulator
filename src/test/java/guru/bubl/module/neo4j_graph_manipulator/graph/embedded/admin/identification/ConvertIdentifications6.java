///*
// * Copyright Vincent Blouin under the GPL License version 3
// */
//
//package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.identification;
//
//import com.google.inject.Guice;
//import com.google.inject.Injector;
//import guru.bubl.module.model.graph.graph_element.GraphElementOperator;
//import guru.bubl.module.model.graph.IdentificationPojo;
//import guru.bubl.module.model.json.IdentificationJson;
//import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jModule;
//import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementFactory;
//import org.junit.Test;
//
//import javax.inject.Inject;
//import java.net.URI;
//import java.sql.Connection;
//import java.sql.ResultSet;
//import java.util.Map;
//
//public class ConvertIdentifications6 {
//
//
//    @Inject
//    Neo4jGraphElementFactory neo4jGraphElementFactory;
//
//    @Inject
//    Connection connection;
//
//
//    @Test
//    public void go() throws Exception {
//        Injector injector = Guice.createInjector(
//                Neo4jModule.forTestingUsingRest()
//        );
//        injector.injectMembers(this);
//        String query = "START graph_element=node(*) " +
//                "WHERE graph_element.identifications is not null " +
//                "RETURN graph_element.uri as graph_element_uri, graph_element.identifications as identifications";
//        ResultSet rs = connection.createStatement().executeQuery(query);
//        Integer nb = 0;
//        while (rs.next()) {
//            System.out.println(rs.getString("graph_element_uri"));
//            if (nb++ % 10 == 0) {
//                System.out.println("nb completed " + nb);
//                System.out.println(rs.getString("identifications"));
//                System.out.println(rs.getString("graph_element_uri"));
//            }
//            Map<URI, IdentificationPojo> identifications = IdentificationJson.fromJson(
//                    rs.getString("identifications")
//            );
//            GraphElementOperator graphElementOperator = neo4jGraphElementFactory.withUri(
//                    URI.create(
//                            rs.getString("graph_element_uri")
//                    )
//            );
//            for (IdentificationPojo identification : identifications.values()) {
//                switch (identification.getType().name()) {
//                    case "same_as":
//                        graphElementOperator.addSameAs(
//                                identification
//                        );
//                    case "type":
//                        graphElementOperator.addType(
//                                identification
//                        );
//                    default:
//                        graphElementOperator.addGenericIdentification(
//                                identification
//                        );
//                }
//            }
//        }
//    }
//}
