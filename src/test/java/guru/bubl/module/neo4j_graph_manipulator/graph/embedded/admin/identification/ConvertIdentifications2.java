///*
// * Copyright Vincent Blouin under the GPL License version 3
// */
//
//package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.identification;
//
//
//import com.google.inject.Guice;
//import com.google.inject.Inject;
//import com.google.inject.Injector;
//import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jModule;
//import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jIdentification;
//import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeOperator;
//import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
//import org.junit.Ignore;
//import org.junit.Test;
//import guru.bubl.module.model.UserUris;
//import guru.bubl.module.model.WholeGraph;
//import guru.bubl.module.model.graph.graph_element.GraphElementOperator;
//import guru.bubl.module.model.graph.Identification;
//import guru.bubl.module.model.graph.edge.EdgeOperator;
//import guru.bubl.module.model.graph.vertex.VertexInSubGraphOperator;
//import guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.AdminOperationsOnDatabase;
//
//import java.util.Iterator;
//
//import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;
//
//@Ignore
//public class ConvertIdentifications2 extends AdminOperationsOnDatabase {
//
////    @Inject
////    WholeGraph wholeGraph;
////
////    @Test
////    public void go(){
////        Injector injector = Guice.createInjector(
////                Neo4jModule.forTestingUsingRest()
////        );
////        injector.injectMembers(this);
////        Iterator<VertexInSubGraphOperator> vertexIt = wholeGraph.getAllVertices();
////        while(vertexIt.hasNext()){
////            Neo4jVertexInSubGraphOperator vertexOperator = (Neo4jVertexInSubGraphOperator) vertexIt.next();
////            convert(
////                    vertexOperator,
////                    vertexOperator.getOwnerUsername()
////            );
////        }
////        Iterator<EdgeOperator> edgeIt = wholeGraph.getAllEdges();
////        while(edgeIt.hasNext()){
////            Neo4jEdgeOperator edgeOperator = (Neo4jEdgeOperator) edgeIt.next();
////            convert(
////                    edgeOperator,
////                    edgeOperator.getOwnerUsername()
////            );
////        }
////    }
////
////    public void convert(GraphElementOperator operator, String owner){
////        for(Identification identification : operator.getIdentifications().values()){
////            Neo4jIdentification identificationOperator = (Neo4jIdentification) identification;
////            if(identificationOperator.uri().toString().contains("http:/graph")){
////                String externalUri = identificationOperator.getExternalResourceUri().toString().replace(
////                        "http:/graph",
////                        owner
////                );
////                queryEngine.query(
////                        identificationOperator.queryPrefix() +
////                                "SET n.external_uri={external_uri}, n.uri={uri}, n.owner={owner}",
////                        map(
////                                "external_uri",
////                                externalUri,
////                                "uri",
////                                new UserUris(owner).generateIdentificationUri().toString(),
////                                "owner",
////                                owner
////                        )
////                );
////            }
////        }
////    }
//}
