///*
// * Copyright Vincent Blouin under the GPL License version 3
// */
//
//package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin;
//
//import com.google.inject.Guice;
//import com.google.inject.Inject;
//import com.google.inject.Injector;
//import guru.bubl.module.model.WholeGraph;
//import guru.bubl.module.model.graph.edge.EdgeOperator;
//import guru.bubl.module.model.graph.vertex.VertexOperator;
//import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jModule;
//import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
//import org.junit.Test;
//
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//
//public class NbPublicNeighbors {
//    @Inject
//    WholeGraph wholeGraph;
//
//    @Inject
//    Connection connection;
//
//    @Test
//    public void go() {
//        Injector injector = Guice.createInjector(
//                Neo4jModule.forTestingUsingRest()
//        );
//        injector.injectMembers(this);
//        wholeGraph.getAllVertices().forEach(this::reviewNbPublicNeighbors);
//    }
//
//    private void reviewNbPublicNeighbors(VertexOperator vertex) {
//        Integer nbPublicNeighbors = 0;
//        for (EdgeOperator edge : vertex.connectedEdges()) {
//            VertexOperator otherVertex = edge.otherVertex(vertex);
//            if (otherVertex.isPublic()) {
//                nbPublicNeighbors++;
//            }
//        }
//        try {
//            String query = String.format(
//                    "%s SET n.%s={1}",
//                    ((Neo4jVertexInSubGraphOperator) vertex).queryPrefix(),
//                    Neo4jVertexInSubGraphOperator.props.nb_public_neighbors
//            );
//            PreparedStatement statement = connection.prepareStatement(
//                    query
//            );
//            statement.setInt(
//                    1, nbPublicNeighbors
//            );
//            statement.execute();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
