/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import com.google.inject.Inject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.FriendlyResource;
import guru.bubl.module.model.graph.GraphElement;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.json.JsonUtils;
import guru.bubl.module.model.search.GraphIndexer;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.Neo4jSubGraphExtractorFactory;

import java.net.URI;
import java.sql.Connection;
import java.util.*;

public class Neo4jGraphIndexer implements GraphIndexer {

    @Inject
    Neo4jSubGraphExtractorFactory subGraphExtractorFactory;

    @Inject
    Neo4jFriendlyResourceFactory neo4jFriendlyResourceFactory;

    @Inject
    Connection connection;

    private Comparator vertexContextComparator = (Comparator<VertexInSubGraphPojo>)
            (vertexA, vertexB) -> vertexA.getNumberOfConnectedEdges() - vertexB.getNumberOfConnectedEdges();

    @Override
    public void indexVertex(VertexOperator vertex) {
        SubGraphPojo subGraph = subGraphExtractorFactory.withCenterVertexDepthAndResultsLimit(
                vertex.uri(),
                1,
                10
        ).load();
        subGraph.vertices().remove(vertex.uri());
        setPrivateAndPublicSearchContextToFriendlyResource(
                sortVerticesByNumberOfChild(subGraph.vertices()),
                sortVerticesByNumberOfChild(subGraph.getPublicVertices()),
                vertex
        );
    }

    @Override
    public void indexRelation(Edge edge) {
        Map<URI, Vertex> sourceAndDestination = new HashMap<>();
        Vertex sourceVertex = edge.sourceVertex();
        Vertex destinationVertex = edge.destinationVertex();
        sourceAndDestination.put(
                sourceVertex.uri(),
                sourceVertex
        );
        sourceAndDestination.put(
                destinationVertex.uri(),
                destinationVertex
        );
        setPrivateAndPublicSearchContextToFriendlyResource(
                sourceAndDestination,
                sourceAndDestination,
                edge
        );
    }

    @Override
    public void indexSchema(SchemaPojo schema) {
        setPrivateAndPublicSearchContextToFriendlyResource(
                schema.getProperties(),
                schema.getProperties(),
                schema
        );
    }

    @Override
    public void indexProperty(GraphElementPojo property, SchemaPojo schema) {
        Map<URI, SchemaPojo> context = new HashMap<>();
        context.put(
                schema.uri(),
                schema
        );
        setPrivateAndPublicSearchContextToFriendlyResource(
                context,
                context,
                property
        );
    }

    @Override
    public void deleteGraphElement(GraphElement graphElement) {

    }

    @Override
    public void commit() {

    }

    private void filterTheContext(Map<URI, ? extends GraphElement> context){
        Integer numberOfContextElement = 0;
        Iterator it = context.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            FriendlyResource friendlyResource = (FriendlyResource) pair.getValue();
            if(numberOfContextElement >= CONTEXT_LIMIT){
                it.remove();
            }
            else if(friendlyResource.label().isEmpty()){
                it.remove();
            }else{
                numberOfContextElement++;
            }
        }
    }

    private void setPrivateAndPublicSearchContextToFriendlyResource(Map<URI, ? extends GraphElement> privateContext, Map<URI, ? extends GraphElement> publicContext, FriendlyResource friendlyResource) {
        Neo4jFriendlyResource neo4jFriendlyResource = neo4jFriendlyResourceFactory.withUri(
                friendlyResource.uri()
        );
        filterTheContext(privateContext);
        filterTheContext(publicContext);
        NoExRun.wrap(() -> {
            String query = String.format(
                    "%s SET n.private_context=@privateContext, " +
                            "n.public_context=@publicContext",
                    neo4jFriendlyResource.queryPrefix()
            );
            NamedParameterStatement statement = new NamedParameterStatement(connection, query);
            statement.setString(
                    "privateContext",
                    convertGraphElementsToContextJsonString(privateContext)
            );
            statement.setString(
                    "publicContext",
                    convertGraphElementsToContextJsonString(publicContext)
            );
            return statement.execute();
        }).get();
    }

    private Map<URI, String> mapOfGraphElementsToMapOfLabels(Map<URI, ? extends GraphElement> mapOfGraphElements) {
        Map<URI, String> mapOfLabels = new LinkedHashMap<>();
        for (GraphElement graphElement : mapOfGraphElements.values()) {
            mapOfLabels.put(
                    graphElement.uri(),
                    graphElement.label()
            );
        }
        return mapOfLabels;
    }

    private String mapOfUrisAndLabelToJsonString(Map<URI, String> mapOfGraphElements) {
        return JsonUtils.getGson().toJson(
                mapOfGraphElements
        );
    }

    private String convertGraphElementsToContextJsonString(Map<URI, ? extends GraphElement> mapOfGraphElements) {
        return mapOfUrisAndLabelToJsonString(
                mapOfGraphElementsToMapOfLabels(
                        mapOfGraphElements
                )
        );
    }

    private Map<URI, VertexInSubGraphPojo> sortVerticesByNumberOfChild(Map<URI, VertexInSubGraphPojo> vertices){
        List<Map.Entry<URI, VertexInSubGraphPojo>> list =
                new LinkedList<>(vertices.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<URI, VertexInSubGraphPojo>>() {
            public int compare(Map.Entry<URI, VertexInSubGraphPojo> o1,
                               Map.Entry<URI, VertexInSubGraphPojo> o2) {
                if(o1.getValue().getNumberOfConnectedEdges() > o2.getValue().getNumberOfConnectedEdges()){
                    return -1;
                }else if(o1.getValue().getNumberOfConnectedEdges() == o2.getValue().getNumberOfConnectedEdges()){
                    return 0;
                }else{
                    return 1;
                }
            }
        });

        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<URI, VertexInSubGraphPojo> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<URI, VertexInSubGraphPojo> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        /*
        //classic iterator example
        for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext(); ) {
            Map.Entry<String, Integer> entry = it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }*/


        return sortedMap;
    }
}
