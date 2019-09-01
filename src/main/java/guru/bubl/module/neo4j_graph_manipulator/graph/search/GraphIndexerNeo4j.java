/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import com.google.inject.Inject;
import guru.bubl.module.model.FriendlyResource;
import guru.bubl.module.model.graph.GraphElement;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.graph.schema.SchemaPojo;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.model.json.JsonUtils;
import guru.bubl.module.model.search.GraphIndexer;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.SubGraphExtractorFactoryNeo4j;
import org.neo4j.driver.v1.Session;

import java.net.URI;
import java.util.*;

import static org.neo4j.driver.v1.Values.parameters;

public class GraphIndexerNeo4j implements GraphIndexer {

    @Inject
    SubGraphExtractorFactoryNeo4j subGraphExtractorFactory;

    @Inject
    FriendlyResourceFactoryNeo4j neo4jFriendlyResourceFactory;

    @Inject
    Session session;

    @Override
    public void indexVertex(VertexOperator vertex) {
        indexWhereContextIsSurroundVertices(
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
        setPrivateFriendsAndPublicSearchContextToFriendlyResource(
                sourceAndDestination,
                sourceAndDestination,
                sourceAndDestination,
                edge
        );
    }

    @Override
    public void indexSchema(SchemaPojo schema) {
        setPrivateFriendsAndPublicSearchContextToFriendlyResource(
                schema.getProperties(),
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
        setPrivateFriendsAndPublicSearchContextToFriendlyResource(
                context,
                context,
                context,
                property
        );
    }

    @Override
    public void indexMeta(IdentifierPojo identifier) {
        if (identifier.gotComments()) {
            String descriptionAsContext = descriptionToContext(
                    identifier.comment()
            );
            setPrivatePublicContext(
                    descriptionAsContext,
                    descriptionAsContext,
                    descriptionAsContext,
                    identifier
            );
        } else {
            indexWhereContextIsSurroundVertices(identifier);
        }
    }

    @Override
    public void deleteGraphElement(GraphElement graphElement) {

    }

    @Override
    public void commit() {

    }

    private void indexWhereContextIsSurroundVertices(FriendlyResource friendlyResource) {
        SubGraphPojo subGraph = subGraphExtractorFactory.withCenterVertexInShareLevels(
                friendlyResource.uri(),
                ShareLevel.allShareLevels
        ).load();
        subGraph.vertices().remove(friendlyResource.uri());
        setPrivateFriendsAndPublicSearchContextToFriendlyResource(
                sortVerticesByNumberOfChild(subGraph.vertices()),
                sortVerticesByNumberOfChild(subGraph.getFriendsAndPublicIndexableVertices()),
                sortVerticesByNumberOfChild(subGraph.getPublicIndexableVertices()),
                friendlyResource
        );
    }

    private void filterTheContext(Map<URI, ? extends GraphElement> context) {
        Integer numberOfContextElement = 0;
        Iterator it = context.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            FriendlyResource friendlyResource = (FriendlyResource) pair.getValue();
            if (numberOfContextElement >= CONTEXT_LIMIT) {
                it.remove();
            } else if (friendlyResource.label().isEmpty()) {
                it.remove();
            } else {
                numberOfContextElement++;
            }
        }
    }

    private void setPrivateFriendsAndPublicSearchContextToFriendlyResource(
            Map<URI, ? extends GraphElement> privateContext,
            Map<URI, ? extends GraphElement> friendsContext,
            Map<URI, ? extends GraphElement> publicContext,
            FriendlyResource friendlyResource) {
        filterTheContext(privateContext);
        filterTheContext(publicContext);
        setPrivatePublicContext(
                convertGraphElementsToContextJsonString(privateContext),
                convertGraphElementsToContextJsonString(friendsContext),
                convertGraphElementsToContextJsonString(publicContext),
                friendlyResource
        );
    }

    private void setPrivatePublicContext(String privateContext, String friendsContext, String publicContext, FriendlyResource friendlyResource) {
        FriendlyResourceNeo4j neo4jFriendlyResource = neo4jFriendlyResourceFactory.withUri(
                friendlyResource.uri()
        );
        session.run(
                neo4jFriendlyResource.queryPrefix() + "SET n.private_context=$privateContext, n.public_context=$publicContext",
                parameters(
                        "uri",
                        neo4jFriendlyResource.uri().toString(),
                        "privateContext",
                        privateContext,
                        "publicContext",
                        publicContext
                )
        );
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

    public static String descriptionToContext(String description) {
        Map<URI, String> context = new HashMap<>();
        context.put(
                URI.create("description"),
                description
        );
        return JsonUtils.getGson().toJson(
                context
        );
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

    private Map<URI, VertexInSubGraphPojo> sortVerticesByNumberOfChild(Map<URI, VertexInSubGraphPojo> vertices) {
        List<Map.Entry<URI, VertexInSubGraphPojo>> list =
                new LinkedList<>(vertices.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<URI, VertexInSubGraphPojo>>() {
            public int compare(Map.Entry<URI, VertexInSubGraphPojo> o1,
                               Map.Entry<URI, VertexInSubGraphPojo> o2) {
                if (o1.getValue().getNumberOfConnectedEdges() > o2.getValue().getNumberOfConnectedEdges()) {
                    return -1;
                } else if (o1.getValue().getNumberOfConnectedEdges() == o2.getValue().getNumberOfConnectedEdges()) {
                    return 0;
                } else {
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
