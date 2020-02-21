/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.tag.Tag;
import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.model.graph.vertex.*;
import guru.bubl.module.model.json.SuggestionJson;
import guru.bubl.module.model.suggestion.SuggestionPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.SubGraphExtractorNeo4j;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.graphdb.Node;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.decrementNbFriendsOrPublicQueryPart;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.incrementNbFriendsOrPublicQueryPart;
import static org.neo4j.driver.v1.Values.parameters;

public class VertexInSubGraphOperatorNeo4j implements VertexInSubGraphOperator, OperatorNeo4j {

    public enum props {
        number_of_connected_edges_property_name,
        nb_public_neighbors,
        nb_friend_neighbors,
        shareLevel,
        is_public,
        suggestions
    }

    protected GraphElementOperatorNeo4j graphElementOperator;
    protected VertexFactoryNeo4j vertexFactory;

    protected EdgeFactoryNeo4j edgeFactory;

    protected GraphElementFactoryNeo4j neo4jGraphElementFactory;
    protected Node node;
    protected Driver driver;


    @Inject
    protected FriendlyResourceFactoryNeo4j friendlyResourceFactory;

    @AssistedInject
    protected VertexInSubGraphOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j neo4jGraphElementFactory,
            Driver driver,
            @Assisted URI uri
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.neo4jGraphElementFactory = neo4jGraphElementFactory;
        this.driver = driver;
        this.graphElementOperator = neo4jGraphElementFactory.withUri(
                uri
        );
    }

    @AssistedInject
    protected VertexInSubGraphOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j neo4jGraphElementFactory,
            Driver driver,
            @Assisted String ownerUserName
    ) {
        this(
                vertexFactory,
                edgeFactory,
                neo4jGraphElementFactory,
                driver,
                new UserUris(ownerUserName).generateVertexUri()
        );
        create();
    }

    @AssistedInject
    protected VertexInSubGraphOperatorNeo4j(
            final VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j neo4jGraphElementFactory,
            Driver driver,
            final @Assisted Set<Vertex> includedVertices,
            final @Assisted Set<Edge> includedEdges
    ) throws IllegalArgumentException {
        this(
                vertexFactory,
                edgeFactory,
                neo4jGraphElementFactory,
                driver,
                new UserUris(
                        includedVertices.iterator().next().getOwnerUsername()
                ).generateVertexUri()
        );
        if (includedVertices.size() <= 1) {
            throw new IllegalArgumentException(
                    "A minimum number of 2 vertices is required to create a vertex from included vertices"
            );
        }
        create();
//        setIncludedVertices(
//                includedVertices
//        );
//        setIncludedEdges(
//                includedEdges
//        );
    }

    @Override
    public boolean hasEdge(Edge edge) {
        FriendlyResourceNeo4j edgeFriendlyResource = friendlyResourceFactory.withUri(
                edge.uri()
        );
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    String.format(
                            "%s, %s, (n)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(edge) RETURN edge",
                            queryPrefix(),
                            edgeFriendlyResource.addToSelectUsingVariableName("edge", "edgeUri")
                    ),
                    parameters(
                            "uri", uri().toString(),
                            "edgeUri", edgeFriendlyResource.uri().toString()
                    )
            );
            return rs.hasNext();
        }
    }

    @Override
    public EdgeOperator getEdgeThatLinksToDestinationVertex(Vertex destinationVertex) {
        FriendlyResourceNeo4j destinationVertexOperator = friendlyResourceFactory.withUri(
                destinationVertex.uri()
        );
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    String.format(
                            "%s, %s, (n)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(r), " +
                                    "(r)-[:SOURCE_VERTEX|DESTINATION_VERTEX]->(d) " +
                                    "RETURN r.uri as uri",
                            queryPrefix(),
                            destinationVertexOperator.addToSelectUsingVariableName("d", "destinationUri")
                    ),
                    parameters(
                            "uri", uri().toString(),
                            "destinationUri", destinationVertexOperator.uri().toString()
                    )
            );
            if (!rs.hasNext()) {
                throw new RuntimeException(
                        "Edge between vertex with " + uri() +
                                " and vertex with uri " + destinationVertex.uri() +
                                " was not found"
                );
            }
            Record record = rs.next();
            return edgeFactory.withUri(URI.create(
                    record.get(
                            "uri"
                    ).asString()
            ));
        }
    }

    @Override
    public Boolean hasDestinationVertex(Vertex destinationVertex) {
        FriendlyResourceNeo4j destinationVertexOperator = friendlyResourceFactory.withUri(
                destinationVertex.uri()
        );
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    String.format(
                            "%s, %s, " +
                                    "(n)<-[:SOURCE_VERTEX]-(r), " +
                                    "(r)-[:DESTINATION_VERTEX]->(d) " +
                                    "RETURN n.uri",
                            queryPrefix(),
                            destinationVertexOperator.addToSelectUsingVariableName("d", "destinationUri")
                    ),
                    parameters(
                            "uri", uri().toString(),
                            "destinationUri", destinationVertexOperator.uri().toString()
                    )
            );
            return rs.hasNext();
        }
    }

    @Override
    public EdgePojo addVertexAndRelation() {
        return addVertexAndRelationIsUnderPatternOrNot(
                new UserUris(
                        getOwnerUsername()
                ).generateVertexUri(),
                null,
                this.isPatternOrUnderPattern()
        );
    }

    @Override
    public EdgePojo addVertexAndRelationWithIds(String vertexId, String edgeId) {
        return addVertexAndRelationWithIdsUnderPatternOrNot(
                vertexId,
                edgeId,
                this.isPatternOrUnderPattern()
        );
    }

    private EdgePojo addVertexAndRelationWithIdsUnderPatternOrNot(String vertexId, String edgeId, Boolean isUnderPattern) {
        UserUris userUri = new UserUris(
                getOwnerUsername()
        );
        URI vertexUri = userUri.vertexUriFromShortId(vertexId);
        if (FriendlyResourceNeo4j.haveElementWithUri(vertexUri, driver)) {
            vertexUri = userUri.generateVertexUri();
        }
        URI edgeUri = userUri.edgeUriFromShortId(edgeId);
        if (FriendlyResourceNeo4j.haveElementWithUri(edgeUri, driver)) {
            edgeUri = userUri.generateEdgeUri();
        }
        return this.addVertexAndRelationIsUnderPatternOrNot(
                vertexUri,
                edgeUri,
                isUnderPattern
        );
    }

    private EdgePojo addVertexAndRelationIsUnderPatternOrNot(URI newVertexUri, URI newEdgeUri, Boolean isUnderPattern) {
        VertexInSubGraphOperatorNeo4j newVertexOperator = vertexFactory.withUri(
                newVertexUri
        );
        this.incrementNumberOfConnectedEdges();
        Boolean isPublic = isUnderPattern || this.isPublic();
        Map<String, Object> properties = map(
                props.number_of_connected_edges_property_name.name(), 1,
                props.nb_public_neighbors.name(), isPublic ? 1 : 0
        );
        if (isUnderPattern) {
            properties.put(
                    "isUnderPattern",
                    true
            );
            properties.put(
                    "shareLevel",
                    ShareLevel.PUBLIC.getIndex()
            );
        }
        VertexPojo newVertex = newVertexOperator.createVertexUsingInitialValues(
                properties
        );
        EdgeOperatorNeo4j edgeOperator = newEdgeUri == null ? edgeFactory.withSourceAndDestinationVertex(
                this,
                newVertexOperator
        ) : edgeFactory.withUriAndSourceAndDestinationVertex(
                newEdgeUri,
                this,
                newVertexOperator
        );
        EdgePojo newEdge = isUnderPattern ?
                edgeOperator.createEdgeWithAdditionalProperties(
                        map("isUnderPattern", true)
                ) :
                edgeOperator.createEdge();

        newEdge.setDestinationVertex(
                new VertexInSubGraphPojo(
                        newVertex
                )
        );
        return newEdge;
    }

    @Override
    public EdgeOperator addRelationToVertex(final VertexOperator destinationVertex) {
        if (this.isPatternOrUnderPattern() || destinationVertex.isPatternOrUnderPattern()) {
            return null;
        }
        EdgeOperator edge = edgeFactory.withSourceAndDestinationVertex(
                this,
                destinationVertex
        );
        ShareLevel sourceShareLevel = this.getShareLevel();
        ShareLevel destinationShareLevel = destinationVertex.getShareLevel();
        if (sourceShareLevel == ShareLevel.FRIENDS && destinationShareLevel == ShareLevel.FRIENDS) {
            edge.createWithShareLevel(ShareLevel.FRIENDS);
        } else if (sourceShareLevel.isPublic() && destinationShareLevel.isPublic()) {
            edge.createWithShareLevel(ShareLevel.PUBLIC_WITH_LINK);
        } else {
            edge.createWithShareLevel(ShareLevel.PRIVATE);
        }
        String query = String.format(
                "MATCH (s:Vertex {uri:$uri}), (d:Vertex {uri:$destinationUri}) " +
                        "SET " +
                        "s.%s=s.%s+1, " +
                        "d.%s=d.%s+1 " +
                        incrementNbFriendsOrPublicQueryPart(destinationShareLevel, "s", "WITH s,d SET ") +
                        incrementNbFriendsOrPublicQueryPart(sourceShareLevel, "d", "WITH s,d SET "),
                props.number_of_connected_edges_property_name,
                props.number_of_connected_edges_property_name,
                props.number_of_connected_edges_property_name,
                props.number_of_connected_edges_property_name
        );
        try (Session session = driver.session()) {
            session.run(
                    query,
                    parameters(
                            "uri",
                            this.uri().toString(),
                            "destinationUri",
                            destinationVertex.uri().toString()
                    )
            );
            return edge;
        }
    }

    @Override
    public EdgeOperator acceptSuggestion(final SuggestionPojo suggestion) {
        return suggestion.isFromComparison() ?
                acceptSuggestionFromComparison(suggestion) :
                acceptSuggestionFromSchema(suggestion);
    }

    private EdgeOperator acceptSuggestionFromSchema(final SuggestionPojo suggestion) {
        VertexOperator newVertex = vertexFactory.withUri(
                new UserUris(
                        getOwnerUsername()
                ).generateVertexUri()
        );
        Edge newEdge = addVertexAndRelationIsUnderPatternOrNot(
                newVertex.uri(),
                null,
                false
        );
        EdgeOperator newEdgeOperator = edgeFactory.withUri(
                newEdge.uri()
        );
        newEdgeOperator.label(
                suggestion.label()
        );

        if (suggestion.getSameAs() != null) {
            newEdgeOperator.addMeta(
                    new TagPojo(
                            suggestion.getSameAs().uri(),
                            new GraphElementPojo(
                                    suggestion.getSameAs()
                            )
                    )
            );
            newVertex.addMeta(
                    new TagPojo(
                            suggestion.getSameAs().uri(),
                            new GraphElementPojo(
                                    suggestion.getSameAs()
                            )
                    )
            );
        }
        if (suggestion.getType() != null) {
            newVertex.addMeta(
                    new TagPojo(
                            suggestion.getType().uri(),
                            new GraphElementPojo(
                                    suggestion.getType()
                            )

                    )
            );
        }
        return newEdgeOperator;
    }

    private EdgeOperator acceptSuggestionFromComparison(final SuggestionPojo suggestion) {
        VertexOperator newVertex = vertexFactory.withUri(
                new UserUris(
                        getOwnerUsername()
                ).generateVertexUri()
        );
        EdgePojo newEdge = addVertexAndRelationIsUnderPatternOrNot(
                newVertex.uri(),
                null,
                false
        );
        EdgeOperator newEdgeOperator = edgeFactory.withUri(
                newEdge.uri()
        );
        newEdgeOperator.label(
                suggestion.label()
        );
        newEdgeOperator.addMeta(
                new TagPojo(
                        suggestion.getSameAs().uri(),
                        new GraphElementPojo(
                                suggestion.getSameAs()
                        )
                )
        );
        newVertex.addMeta(
                new TagPojo(
                        suggestion.getType().uri(),
                        new GraphElementPojo(
                                suggestion.getType()
                        )
                )
        );
        newVertex.label(
                suggestion.getType().label()
        );
        return newEdgeOperator;
    }


    @Override
    public void remove() {
        //todo batch
        graphElementOperator.removeAllIdentifications();
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() +
                            "MATCH " +
                            "(n)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(e),  " +
                            "(e)-[:IDENTIFIED_TO]->(i) " +
                            "SET " +
                            "i.nb_references = i.nb_references - 1",
                    parameters(
                            "uri", this.uri().toString()
                    )
            );
            ShareLevel shareLevel = getShareLevel();
            String decrementQueryPart = shareLevel == ShareLevel.PRIVATE ? "" :
                    decrementNbFriendsOrPublicQueryPart(shareLevel, "v", ", ") + " ";
            session.run(
                    queryPrefix() +
                            "OPTIONAL MATCH " +
                            "(n)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(e), " +
                            "(e)-[:SOURCE_VERTEX|DESTINATION_VERTEX]->(v) " +
                            "SET " +
                            "v.number_of_connected_edges_property_name = " +
                            "v.number_of_connected_edges_property_name - 1 " +
                            decrementQueryPart +
                            "WITH e, n " +
                            "OPTIONAL MATCH (e)-[e_r]-(), " +
                            "(n)-[v_r]-() " +
                            "DELETE " +
                            "v_r, n, " +
                            "e_r, e",
                    parameters(
                            "uri",
                            this.uri().toString()
                    )
            );
        }
    }

    @Override
    public Map<URI, EdgeOperator> connectedEdges() {
        Map<URI, EdgeOperator> edges = new HashMap<>();
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    queryPrefix() +
                            "MATCH (n)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(edge) " +
                            "RETURN edge.uri as uri",
                    parameters(
                            "uri",
                            this.uri().toString()
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                URI edgeUri = URI.create(
                        record.get(
                                "uri"
                        ).asString()
                );
                edges.put(
                        edgeUri,
                        edgeFactory.withUri(
                                edgeUri
                        )
                );
            }
            return edges;
        }
    }

    @Override
    public VertexOperator forkForUserUsingCache(User user, Vertex cache) {
        VertexOperator clone = vertexFactory.withUri(
                new UserUris(
                        user.username()
                ).generateVertexUri()
        );
        graphElementOperator.forkUsingCreationPropertiesAndCache(
                clone,
                map(
                        props.number_of_connected_edges_property_name.name(),
                        cache.getNumberOfConnectedEdges()
                ),
                cache
        );
        return clone;
    }

    @Override
    public Integer getNumberOfConnectedEdges() {
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    String.format(
                            "%sRETURN n.%s as result",
                            queryPrefix(),
                            props.number_of_connected_edges_property_name
                    ),
                    parameters(
                            "uri",
                            uri().toString()
                    )
            );
            Record record = rs.next();
            return record.get("result").asInt();
        }
    }

    @Override
    public Integer getNbPublicNeighbors() {
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    String.format(
                            "%sRETURN n.nb_public_neighbors as result",
                            queryPrefix()
                    ),
                    parameters(
                            "uri",
                            this.uri().toString()
                    )
            );
            Record record = rs.next();
            return record.get("result").asInt();
        }
    }

    @Override
    public Integer getNbFriendNeighbors() {
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    queryPrefix() + "RETURN n.nb_friend_neighbors as result",
                    parameters(
                            "uri",
                            uri().toString()
                    )
            );
            Record record = rs.next();
            return record.get("result").asInt();
        }
    }

    @Override
    public void setNumberOfConnectedEdges(Integer numberOfConnectedEdges) {
        try (Session session = driver.session()) {
            session.run(
                    String.format(
                            "%s SET n.%s=$nbConnectedEdges",
                            queryPrefix(),
                            props.number_of_connected_edges_property_name
                    ),
                    parameters(
                            "uri",
                            uri().toString(),
                            "nbConnectedEdges",
                            numberOfConnectedEdges
                    )
            );
        }
    }

    @Override
    public void setNumberOfPublicConnectedEdges(Integer nbPublicNeighbors) {
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "SET n.nb_public_neighbors=$nbPublicNeighbors",
                    parameters(
                            "uri",
                            uri().toString(),
                            "nbPublicNeighbors",
                            nbPublicNeighbors
                    )
            );
        }
    }

    @Override
    public void setNbFriendNeighbors(Integer nbFriendNeighbors) {
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "SET n.nb_friend_neighbors=$nbFriendNeighbors",
                    parameters(
                            "uri",
                            uri().toString(),
                            "nbFriendNeighbors",
                            nbFriendNeighbors
                    )
            );
        }
    }

    protected void incrementNumberOfConnectedEdges() {
        try (Session session = driver.session()) {
            session.run(
                    String.format(
                            "%s SET n.%s= n.%s + 1",
                            queryPrefix(),
                            props.number_of_connected_edges_property_name,
                            props.number_of_connected_edges_property_name
                    ),
                    parameters(
                            "uri", uri().toString()
                    )
            );
        }
    }

    @Override
    public void setSuggestions(Map<URI, SuggestionPojo> suggestions) {
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "SET n.suggestions=$suggestions",
                    parameters(
                            "uri",
                            this.uri().toString(),
                            "suggestions",
                            SuggestionJson.multipleToJson(suggestions).toString()
                    )
            );
        }
    }

    @Override
    public void addSuggestions(final Map<URI, SuggestionPojo> suggestions) {
        Map<URI, SuggestionPojo> current = getSuggestions();
        current.putAll(suggestions);
        setSuggestions(current);
    }

    @Override
    public Map<URI, SuggestionPojo> getSuggestions() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() + "RETURN n.suggestions as suggestions",
                    parameters(
                            "uri",
                            this.uri().toString()
                    )
            ).single();
            return record.get(
                    "suggestions"
            ).asObject() == null ?
                    new HashMap<URI, SuggestionPojo>() :
                    SuggestionJson.fromJsonArray(
                            record.get(
                                    "suggestions"
                            ).asString()
                    );
        }
    }

    @Override
    public void removeIdentification(Tag identification) {
        graphElementOperator.removeIdentification(identification);
    }

    @Override
    public Map<URI, TagPojo> addMeta(Tag friendlyResource) {
        return graphElementOperator.addMeta(friendlyResource);
    }

    @Override
    public Map<URI, TagPojo> getIdentifications() {
        return graphElementOperator.getIdentifications();
    }

    @Override
    public Boolean isPublic() {
        return graphElementOperator.isPublic();
    }

    @Override
    public void makePublic() {
        setShareLevel(ShareLevel.PUBLIC);
    }

    @Override
    public void makePrivate() {
        setShareLevel(ShareLevel.PRIVATE);
    }

    @Override
    public Map<URI, Vertex> getIncludedVertices() {
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    String.format(
                            "%sMATCH (n)-[:HAS_INCLUDED_VERTEX]->(included_vertex) RETURN %s'dummy_return'",
                            queryPrefix(),
                            SubGraphExtractorNeo4j.includedElementQueryPart("included_vertex")
                    ),
                    parameters(
                            "uri", this.uri().toString()
                    )
            );
            Map<URI, Vertex> includedVertices = new HashMap<>();
            while (rs.hasNext()) {
                Record record = rs.next();
                URI uri = URI.create(
                        record.get("included_vertex.uri").asString()
                );
                includedVertices.put(
                        uri,
                        new VertexInSubGraphPojo(
                                uri,
                                record.get(
                                        "included_vertex." + FriendlyResourceNeo4j.props.label.toString()
                                ).asString()
                        )
                );
            }
            return includedVertices;
        }
    }

    @Override
    public Map<URI, Edge> getIncludedEdges() {
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    String.format(
                            "%sMATCH n-[:HAS_INCLUDED_EDGE]->included_edge RETURN %s'dummy_return'",
                            queryPrefix(),
                            SubGraphExtractorNeo4j.includedElementQueryPart("included_edge")
                    ),
                    parameters(
                            "uri",
                            uri().toString()
                    )
            );
            Map<URI, Edge> includedEdge = new HashMap<>();
            while (rs.hasNext()) {
                Record record = rs.next();
                URI uri = URI.create(
                        record.get("included_edge.uri").asString()
                );
                includedEdge.put(
                        uri,
                        new EdgePojo(
                                uri,
                                record.get(
                                        "included_edge." + FriendlyResourceNeo4j.props.label.toString()
                                ).asString()
                        )
                );
            }
            return includedEdge;
        }
    }


//    public void setIncludedVertices(Set<Vertex> includedVertices) {
//        for (Vertex vertex : includedVertices) {
//            getNode().createRelationshipTo(
//                    vertexFactory.withUri(vertex.uri()).getNode(),
//                    Relationships.HAS_INCLUDED_VERTEX
//            );
//        }
//    }
//
//    public void setIncludedEdges(Set<Edge> includedEdges) {
//        for (Edge edge : includedEdges) {
//            getNode().createRelationshipTo(
//                    edgeFactory.withUri(edge.uri()).getNode(),
//                    Relationships.HAS_INCLUDED_EDGE
//            );
//        }
//    }

    @Override
    public Date creationDate() {
        return graphElementOperator.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return graphElementOperator.lastModificationDate();
    }

    @Override
    public String getOwnerUsername() {
        return graphElementOperator.getOwnerUsername();
    }

    @Override
    public URI uri() {
        return graphElementOperator.uri();
    }

    @Override
    public String label() {
        return graphElementOperator.label();
    }

    @Override
    public Set<Image> images() {
        return graphElementOperator.images();
    }

    @Override
    public Boolean gotImages() {
        return graphElementOperator.gotImages();
    }

    @Override
    public String comment() {
        return graphElementOperator.comment();
    }

    @Override
    public void comment(String comment) {
        graphElementOperator.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return graphElementOperator.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        graphElementOperator.addImages(
                images
        );
    }

    @Override
    public void label(String label) {
        graphElementOperator.label(label);
    }

    @Override
    public boolean hasLabel() {
        return graphElementOperator.hasLabel();
    }

    @Override
    public String getColors() {
        return graphElementOperator.getColors();
    }

    @Override
    public String getFont() {
        return graphElementOperator.getFont();
    }

    @Override
    public void setColors(String colors) {
        graphElementOperator.setColors(colors);
    }

    @Override
    public void setFont(String font) {
        graphElementOperator.setFont(font);
    }

    @Override
    public void setChildrenIndex(String childrenIndex) {
        graphElementOperator.setChildrenIndex(
                childrenIndex
        );
    }

    @Override
    public Boolean isUnderPattern() {
        return graphElementOperator.isUnderPattern();
    }

    @Override
    public Boolean isPatternOrUnderPattern() {
        return graphElementOperator.isPatternOrUnderPattern();
    }

    @Override
    public String getChildrenIndex() {
        return graphElementOperator.getChildrenIndex();
    }

    @Override
    public URI getPatternUri() {
        return graphElementOperator.getPatternUri();
    }

    @Override
    public boolean equals(Object vertexToCompareAsObject) {
        return graphElementOperator.equals(vertexToCompareAsObject);
    }

    @Override
    public int hashCode() {
        return graphElementOperator.hashCode();
    }

    @Override
    public void create() {
        createUsingInitialValues(map());
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        createVertexUsingInitialValues(
                values
        );
    }

    @Override
    public Boolean mergeTo(VertexOperator vertexOperator) {
        if (this.isPatternOrUnderPattern() || vertexOperator.isPatternOrUnderPattern()) {
            return false;
        }
        this.connectedEdges().values().forEach(
                (edge) -> {
                    if (edge.destinationVertex().equals(this)) {
                        edge.changeDestinationVertex(vertexOperator);
                    } else {
                        edge.changeSourceVertex(vertexOperator);
                    }
                }
        );
        this.getIdentifications().values().forEach((tag) -> {
            vertexOperator.addMeta(tag);
        });
        this.remove();
        return true;
    }

    @Override
    public void setShareLevel(ShareLevel shareLevel) {
        ShareLevel previousShareLevel = this.getShareLevel();
        String decrementQueryPart = decrementNbFriendsOrPublicQueryPart(previousShareLevel, "d", "SET ");
        String incrementQueryPart = incrementNbFriendsOrPublicQueryPart(shareLevel, "d", "SET ");
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix()
                            + "SET n.shareLevel=$shareLevel " +
                            "WITH n " +
                            "MATCH (n)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]->(e), " +
                            "(e)<-[:SOURCE_VERTEX|DESTINATION_VERTEX]->(d) " +
                            decrementQueryPart + " " +
                            incrementQueryPart + " " +
                            "WITH d,n,e " +
                            "SET e.shareLevel = CASE WHEN (n.shareLevel <= d.shareLevel) THEN n.shareLevel ELSE d.shareLevel END",
                    parameters(
                            "uri", uri().toString(),
                            "shareLevel", shareLevel.getIndex()
                    )
            );
        }
    }

    @Override
    public Boolean makePattern() {
        if (isPatternOrUnderPattern()) {
            return false;
        }
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "SET n:Pattern,n.nbPatternUsage=0 " +
                            "WITH n " +
                            "CALL apoc.path.subgraphAll(n, {relationshipFilter:'SOURCE_VERTEX, DESTINATION_VERTEX'}) YIELD nodes " +
                            "UNWIND nodes as s " +
                            "SET s.shareLevel=40," +
                            "s.isUnderPattern=true," +
                            "s.nb_public_neighbors = s.number_of_connected_edges_property_name," +
                            "s.nb_friend_neighbors = 0 " +
                            "WITH s,n " +
                            "REMOVE n.isUnderPattern " +
                            "WITH s " +
                            "MATCH (s)-[:IDENTIFIED_TO]->(tag) " +
                            "SET tag.shareLevel=40"
                    ,
                    parameters(
                            "uri", uri().toString()
                    )
            );
        }
        return true;
    }

    @Override
    public void undoPattern() {
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "remove n:Pattern " +
                            "WITH n " +
                            "CALL apoc.path.subgraphAll(n, {relationshipFilter:'SOURCE_VERTEX, DESTINATION_VERTEX'}) YIELD nodes " +
                            "UNWIND nodes as s " +
                            "REMOVE s.isUnderPattern",
                    parameters(
                            "uri", uri().toString()
                    )
            );
        }
    }

    @Override
    public Integer getNbPatternUsage() {
        try (Session session = driver.session()) {
            return session.run(
                    queryPrefix() + "RETURN n.nbPatternUsage",
                    parameters(
                            "uri", uri().toString()
                    )
            ).single().get("n.nbPatternUsage").asInt();
        }
    }

    @Override
    public Boolean isPattern() {
        try (Session session = driver.session()) {
            return session.run(
                    queryPrefix() + "RETURN 'Pattern' IN LABELS(n) as isPattern",
                    parameters(
                            "uri", uri().toString()
                    )
            ).single().get("isPattern").asBoolean();
        }
    }

    @Override
    public ShareLevel getShareLevel() {
        return graphElementOperator.getShareLevel();
    }

    private VertexPojo createVertexUsingInitialValues(Map<String, Object> values) {
        Map<String, Object> props = addCreationProperties(
                values
        );
        VertexPojo vertexPojo = pojoFromCreationProperties(
                props
        );
        try (Session session = driver.session()) {
            session.run(
                    "CREATE(n:Resource:GraphElement:Vertex $vertex)",
                    parameters(
                            "vertex",
                            props
                    )
            );
            return vertexPojo;
        }
    }

    @Override
    public String queryPrefix() {
        return graphElementOperator.queryPrefix();
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Map<String, Object> newMap = map(
                props.shareLevel.name(), ShareLevel.PRIVATE.getIndex(),
                props.number_of_connected_edges_property_name.name(), 0,
                props.nb_public_neighbors.name(), 0,
                props.nb_friend_neighbors.name(), 0
        );
        newMap.putAll(
                map
        );
        newMap = graphElementOperator.addCreationProperties(
                newMap
        );
        return newMap;
    }

    public VertexPojo pojoFromCreationProperties(Map<String, Object> creationProperties) {
        VertexPojo vertex = new VertexPojo(
                graphElementOperator.pojoFromCreationProperties(
                        creationProperties
                )
        );
        if (creationProperties.containsKey(props.nb_public_neighbors.name())) {
            vertex.setNbPublicNeighbors(
                    (Integer) creationProperties.get(
                            props.nb_public_neighbors.name()
                    )
            );
        }
        return vertex;
    }
}
