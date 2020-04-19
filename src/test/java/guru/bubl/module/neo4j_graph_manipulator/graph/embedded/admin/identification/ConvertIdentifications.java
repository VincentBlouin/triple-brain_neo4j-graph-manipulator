///*
// * Copyright Vincent Blouin under the GPL License version 3
// */
//
//package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.identification;
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
//import guru.bubl.module.model.FriendlyResource;
//import guru.bubl.module.model.UserUris;
//import guru.bubl.module.model.WholeGraph;
//import guru.bubl.module.model.graph.graph_element.GraphElementOperator;
//import guru.bubl.module.model.graph.Identification;
//import guru.bubl.module.model.graph.edge.EdgeOperator;
//import guru.bubl.module.model.graph.vertex.VertexInSubGraphOperator;
//import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
//import guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.AdminOperationsOnDatabase;
//
//import java.util.Iterator;
//
//import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;
//
//@Ignore
//public class ConvertIdentifications extends AdminOperationsOnDatabase {
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
////            convertGraphElement(
////                    vertexOperator.queryPrefix(),
////                    vertexOperator
////            );
////        }
////        Iterator<EdgeOperator> edgeIt = wholeGraph.getAllEdges();
////        while(edgeIt.hasNext()){
////            Neo4jEdgeOperator edgeOperator = (Neo4jEdgeOperator) edgeIt.next();
////            convertGraphElement(
////                    edgeOperator.queryPrefix(),
////                    edgeOperator
////            );
////        }
////    }
////
////    private void convertGraphElement(String queryPrefix, GraphElementOperator operator){
////        convert(queryPrefix, operator);
////        for(Identification identification : operator.getIdentifications().values()){
////            Neo4jIdentification identificationOperator = (Neo4jIdentification) identification;
////            String ownerName = UserUris.ownerUserNameFromUri(
////                    identificationOperator.uri()
////            );
////            queryEngine.query(
////                    identificationOperator.queryPrefix() +
////                            "SET n.external_uri={external_uri}, n.uri={uri}",
////                    map(
////                            "external_uri",
////                            identificationOperator.uri().toString(),
////                            "uri",
////                            new UserUris(ownerName).generateIdentificationUri().toString()
////                    )
////            );
////            convert(identificationOperator.queryPrefix(), identification);
////        }
////    }
////
////    private void convert(String queryPrefix, FriendlyResource friendlyResource){
////        queryEngine.query(
////                queryPrefix +
////                        "SET n.owner={owner}",
////                map(
////                        Neo4jFriendlyResource.props.owner.name(),
////                        UserUris.ownerUserNameFromUri(friendlyResource.uri())
////                )
////        );
////    }
//}
