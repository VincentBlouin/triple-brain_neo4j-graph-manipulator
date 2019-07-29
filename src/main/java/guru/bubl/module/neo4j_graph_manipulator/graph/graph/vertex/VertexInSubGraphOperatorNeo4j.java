/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.graph.vertex.*;
import guru.bubl.module.model.json.SuggestionJson;
import guru.bubl.module.model.suggestion.SuggestionPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.SubGraphExtractorNeo4j;
import org.neo4j.graphdb.Node;

import java.net.URI;
import java.sql.*;
import java.util.Date;
import java.util.*;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.decrementNbFriendsOrPublicQueryPart;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.incrementNbFriendsOrPublicQueryPart;

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
    protected Connection connection;


    @Inject
    protected FriendlyResourceFactoryNeo4j friendlyResourceFactory;

    @AssistedInject
    protected VertexInSubGraphOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j neo4jGraphElementFactory,
            Connection connection,
            @Assisted Node node
    ) {
        this(
                vertexFactory,
                edgeFactory,
                neo4jGraphElementFactory,
                connection,
                neo4jGraphElementFactory.withNode(
                        node
                ).uri()
        );
        this.node = node;
    }

    @AssistedInject
    protected VertexInSubGraphOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j neo4jGraphElementFactory,
            Connection connection,
            @Assisted URI uri
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.neo4jGraphElementFactory = neo4jGraphElementFactory;
        this.connection = connection;
        this.graphElementOperator = neo4jGraphElementFactory.withUri(
                uri
        );
    }

    @AssistedInject
    protected VertexInSubGraphOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j neo4jGraphElementFactory,
            Connection connection,
            @Assisted String ownerUserName
    ) {
        this(
                vertexFactory,
                edgeFactory,
                neo4jGraphElementFactory,
                connection,
                new UserUris(ownerUserName).generateVertexUri()
        );
        create();
    }

    @AssistedInject
    protected VertexInSubGraphOperatorNeo4j(
            final VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j neo4jGraphElementFactory,
            Connection connection,
            final @Assisted Set<Vertex> includedVertices,
            final @Assisted Set<Edge> includedEdges
    ) throws IllegalArgumentException {
        this(
                vertexFactory,
                edgeFactory,
                neo4jGraphElementFactory,
                connection,
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
        setIncludedVertices(
                includedVertices
        );
        setIncludedEdges(
                includedEdges
        );
    }

    @Override
    public boolean hasEdge(Edge edge) {
        FriendlyResourceNeo4j edgeFriendlyResource = friendlyResourceFactory.withUri(
                edge.uri()
        );
        String query = String.format(
                "%s, %sMATCH n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-edge RETURN edge",
                queryPrefix(),
                edgeFriendlyResource.addToSelectUsingVariableName("edge")
        );
        return NoEx.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            return rs.next();
        }).get();
    }

    @Override
    public EdgeOperator getEdgeThatLinksToDestinationVertex(Vertex destinationVertex) {
        FriendlyResourceNeo4j destinationVertexOperator = friendlyResourceFactory.withUri(
                destinationVertex.uri()
        );
        return NoEx.wrap(() -> {
            String query = String.format(
                    "%s, %s MATCH n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-r, " +
                            "r-[:SOURCE_VERTEX|DESTINATION_VERTEX]->d " +
                            "RETURN r.uri as uri",
                    queryPrefix(),
                    destinationVertexOperator.addToSelectUsingVariableName("d")
            );
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(
                    query
            );
            if (!rs.next()) {
                throw new RuntimeException(
                        "Edge between vertex with " + uri() +
                                " and vertex with uri " + destinationVertex.uri() +
                                " was not found"
                );
            }
            return edgeFactory.withUri(URI.create(
                    rs.getString(
                            "uri"
                    )
            ));
        }).get();
    }

    @Override
    public Boolean hasDestinationVertex(Vertex destinationVertex) {
        FriendlyResourceNeo4j destinationVertexOperator = friendlyResourceFactory.withUri(
                destinationVertex.uri()
        );
        String query = String.format(
                "%s, %s " +
                        "MATCH n<-[:SOURCE_VERTEX]-r, " +
                        "r-[:DESTINATION_VERTEX]->d " +
                        "RETURN n.uri",
                queryPrefix(),
                destinationVertexOperator.addToSelectUsingVariableName("d")
        );

        return NoEx.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            return rs.next();
        }).get();
    }

    @Override
    public EdgePojo addVertexAndRelation() {
        return addVertexAndRelationToTheLeftOrNotAction(
                this,
                new UserUris(
                        getOwnerUsername()
                ).generateVertexUri(),
                null
        );
    }

    @Override
    public EdgePojo addVertexAndRelationWithIds(String vertexId, String edgeId) {
        UserUris userUri = new UserUris(
                getOwnerUsername()
        );
        URI vertexUri = userUri.vertexUriFromShortId(vertexId);
        if (FriendlyResourceNeo4j.haveElementWithUri(vertexUri, connection)) {
            vertexUri = userUri.generateVertexUri();
        }
        URI edgeUri = userUri.edgeUriFromShortId(edgeId);
        if (FriendlyResourceNeo4j.haveElementWithUri(edgeUri, connection)) {
            edgeUri = userUri.generateEdgeUri();
        }
        return this.addVertexAndRelationToTheLeftOrNotAction(
                this,
                vertexUri,
                edgeUri
        );
    }

    private EdgePojo addVertexAndRelationToTheLeftOrNotAction(VertexInSubGraphOperatorNeo4j self, URI newVertexUri, URI newEdgeUri) {
        VertexInSubGraphOperatorNeo4j newVertexOperator = vertexFactory.withUri(
                newVertexUri
        );
        self.incrementNumberOfConnectedEdges();
        VertexPojo newVertex = newVertexOperator.createVertexUsingInitialValues(
                map(
                        props.number_of_connected_edges_property_name.name(), 1,
                        props.nb_public_neighbors.name(), self.isPublic() ? 1 : 0
                )
        );
        EdgeOperatorNeo4j edgeOperator = newEdgeUri == null ? edgeFactory.withSourceAndDestinationVertex(
                self,
                newVertexOperator
        ) : edgeFactory.withUriAndSourceAndDestinationVertex(
                newEdgeUri,
                self,
                newVertexOperator
        );
        EdgePojo newEdge = edgeOperator.createEdge();
        newEdge.setDestinationVertex(
                new VertexInSubGraphPojo(
                        newVertex
                )
        );
        return newEdge;
    }

    @Override
    public EdgeOperator addRelationToVertex(final VertexOperator destinationVertex) {
        //todo batch
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
                "MATCH (s:%s {uri:'%s'}), (d:%s {uri:'%s'}) " +
                        "SET " +
                        "s.%s=s.%s+1, " +
                        "d.%s=d.%s+1 " +
                        incrementNbFriendsOrPublicQueryPart(destinationShareLevel, "s", "WITH s,d SET ") +
                        incrementNbFriendsOrPublicQueryPart(sourceShareLevel, "d", "WITH s,d SET "),
                GraphElementType.vertex,
                this.uri(),
                GraphElementType.vertex,
                destinationVertex.uri(),
                props.number_of_connected_edges_property_name,
                props.number_of_connected_edges_property_name,
                props.number_of_connected_edges_property_name,
                props.number_of_connected_edges_property_name
        );
        NoEx.wrap(() -> connection.createStatement().executeQuery(
                query
        )).get();
        return edge;
        //todo endbatch
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
        Edge newEdge = addVertexAndRelationToTheLeftOrNotAction(
                this,
                newVertex.uri(),
                null
        );
        EdgeOperator newEdgeOperator = edgeFactory.withUri(
                newEdge.uri()
        );
        newEdgeOperator.label(
                suggestion.label()
        );

        if (suggestion.getSameAs() != null) {
            newEdgeOperator.addMeta(
                    new IdentifierPojo(
                            suggestion.getSameAs().uri(),
                            suggestion.getSameAs()
                    )
            );
            newVertex.addMeta(
                    new IdentifierPojo(
                            suggestion.getSameAs().uri(),
                            suggestion.getSameAs()
                    )
            );
        }
        if (suggestion.getType() != null) {
            newVertex.addMeta(
                    new IdentifierPojo(
                            suggestion.getType().uri(),
                            suggestion.getType()
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
        EdgePojo newEdge = addVertexAndRelationToTheLeftOrNotAction(
                this,
                newVertex.uri(),
                null
        );
        EdgeOperator newEdgeOperator = edgeFactory.withUri(
                newEdge.uri()
        );
        newEdgeOperator.label(
                suggestion.label()
        );
        newEdgeOperator.addMeta(
                new IdentifierPojo(
                        suggestion.getSameAs().uri(),
                        suggestion.getSameAs()
                )
        );
        newVertex.addMeta(
                new IdentifierPojo(
                        suggestion.getType().uri(),
                        suggestion.getType()
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
        NoEx.wrap(() -> {
            graphElementOperator.removeAllIdentifications();
            connection.createStatement().executeQuery(
                    queryPrefix() +
                            "MATCH " +
                            "n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(e),  " +
                            "e-[:IDENTIFIED_TO]->(i) " +
                            "SET " +
                            "i.nb_references = i.nb_references - 1"
            );
            ShareLevel shareLevel = getShareLevel();
            String decrementQueryPart = shareLevel == ShareLevel.PRIVATE ? "" :
                    decrementNbFriendsOrPublicQueryPart(shareLevel, "v", ", ") + " ";
            return connection.createStatement().executeQuery(
                    queryPrefix() +
                            "OPTIONAL MATCH " +
                            "n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(e), " +
                            "e-[:SOURCE_VERTEX|DESTINATION_VERTEX]->(v) " +
                            "SET " +
                            "v.number_of_connected_edges_property_name = " +
                            "v.number_of_connected_edges_property_name - 1 " +
                            decrementQueryPart +
                            "WITH e, n " +
                            "OPTIONAL MATCH e-[e_r]-(), " +
                            "n-[v_r]-() " +
                            "DELETE " +
                            "v_r, n, " +
                            "e_r, e"
            );
        }).get();
        //todo endbatch
    }

    @Override
    public Map<URI, EdgeOperator> connectedEdges() {
        Map<URI, EdgeOperator> edges = new HashMap<>();
        String query = queryPrefix() +
                "MATCH n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(edge) " +
                "RETURN edge.uri as uri";
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            while (rs.next()) {
                URI edgeUri = URI.create(
                        rs.getString(
                                "uri"
                        )
                );
                edges.put(
                        edgeUri,
                        edgeFactory.withUri(
                                edgeUri
                        )
                );
            }
            return edges;
        }).get();
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
        String query = String.format(
                "%sreturn n.%s as result",
                queryPrefix(),
                props.number_of_connected_edges_property_name
        );
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            return Integer.valueOf(
                    rs.getString("result")
            );
        }).get();
    }

    @Override
    public Integer getNbPublicNeighbors() {
        String query = String.format(
                "%sreturn n.%s as result",
                queryPrefix(),
                props.nb_public_neighbors
        );
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            return Integer.valueOf(
                    rs.getString("result")
            );
        }).get();
    }

    @Override
    public Integer getNbFriendNeighbors() {
        String query = String.format(
                "%sreturn n.%s as result",
                queryPrefix(),
                props.nb_friend_neighbors.name()
        );
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            return Integer.valueOf(
                    rs.getString("result")
            );
        }).get();
    }

    @Override
    public void setNumberOfConnectedEdges(Integer numberOfConnectedEdges) {
        NoEx.wrap(() -> {
            String query = String.format(
                    "%s SET n.%s={1}",
                    queryPrefix(),
                    props.number_of_connected_edges_property_name
            );
            PreparedStatement statement = connection.prepareStatement(
                    query
            );
            statement.setInt(
                    1, numberOfConnectedEdges
            );
            return statement.execute();
        }).get();
    }

    @Override
    public void setNumberOfPublicConnectedEdges(Integer nbPublicNeighbors) {
        NoEx.wrap(() -> {
            String query = String.format(
                    "%s SET n.%s={1}",
                    queryPrefix(),
                    props.nb_public_neighbors
            );
            PreparedStatement statement = connection.prepareStatement(
                    query
            );
            statement.setInt(
                    1, nbPublicNeighbors
            );
            return statement.execute();
        }).get();
    }

    @Override
    public void setNbFriendNeighbors(Integer nbFriendNeighbors) {
        NoEx.wrap(() -> {
            String query = String.format(
                    "%s SET n.%s={1}",
                    queryPrefix(),
                    props.nb_friend_neighbors
            );
            PreparedStatement statement = connection.prepareStatement(
                    query
            );
            statement.setInt(
                    1, nbFriendNeighbors
            );
            return statement.execute();
        }).get();
    }

    protected void incrementNumberOfConnectedEdges() {
        NoEx.wrap(() -> {
            Statement statement = connection.createStatement();
            String query = String.format(
                    "%s SET n.%s= n.%s + 1",
                    queryPrefix(),
                    props.number_of_connected_edges_property_name,
                    props.number_of_connected_edges_property_name
            );
            statement.executeUpdate(
                    query
            );
            statement.close();
            return null;
        }).get();
    }

    @Override
    public void setSuggestions(Map<URI, SuggestionPojo> suggestions) {
        String query = String.format(
                "%sSET n.%s= {1} ",
                queryPrefix(),
                props.suggestions
        );
        NoEx.wrap(() -> {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(
                    1,
                    SuggestionJson.multipleToJson(suggestions).toString()
            );
            return statement.execute();
        }).get();
    }

    @Override
    public void addSuggestions(final Map<URI, SuggestionPojo> suggestions) {
        Map<URI, SuggestionPojo> current = getSuggestions();
        current.putAll(suggestions);
        setSuggestions(current);
    }

    @Override
    public Map<URI, SuggestionPojo> getSuggestions() {
        String query = String.format(
                "%sreturn n.`%s` as suggestions",
                queryPrefix(),
                props.suggestions
        );
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            String suggestionsStr = rs.getString(
                    "suggestions"
            );
            return suggestionsStr == null ?
                    new HashMap<URI, SuggestionPojo>() :
                    SuggestionJson.fromJsonArray(
                            suggestionsStr
                    );
        }).get();
    }

    @Override
    public void removeIdentification(Identifier identification) {
        graphElementOperator.removeIdentification(identification);
    }

    @Override
    public Map<URI, IdentifierPojo> addMeta(Identifier friendlyResource) {
        return graphElementOperator.addMeta(friendlyResource);
    }

    @Override
    public Map<URI, IdentifierPojo> getIdentifications() {
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
        String query = String.format(
                "%sMATCH n-[:%s]->included_vertex RETURN %s'dummy_return'",
                queryPrefix(),
                Relationships.HAS_INCLUDED_VERTEX,
                SubGraphExtractorNeo4j.includedElementQueryPart("included_vertex")
        );
        return NoEx.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(
                    query
            );
            Map<URI, Vertex> includedVertices = new HashMap<>();
            while (rs.next()) {
                URI uri = URI.create(
                        rs.getString("included_vertex.uri")
                );
                includedVertices.put(
                        uri,
                        new VertexInSubGraphPojo(
                                uri,
                                rs.getString(
                                        "included_vertex." + FriendlyResourceNeo4j.props.label.toString()
                                )
                        )
                );
            }
            return includedVertices;
        }).get();
    }

    @Override
    public Map<URI, Edge> getIncludedEdges() {
        String query = String.format(
                "%sMATCH n-[:%s]->included_edge RETURN %s'dummy_return'",
                queryPrefix(),
                Relationships.HAS_INCLUDED_EDGE,
                SubGraphExtractorNeo4j.includedElementQueryPart("included_edge")
        );
        return NoEx.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(
                    query
            );
            Map<URI, Edge> includedEdge = new HashMap<>();
            while (rs.next()) {
                URI uri = URI.create(
                        rs.getString("included_edge.uri")
                );
                includedEdge.put(
                        uri,
                        new EdgePojo(
                                uri,
                                rs.getString(
                                        "included_edge." + FriendlyResourceNeo4j.props.label.toString()
                                )
                        )
                );
            }
            return includedEdge;
        }).get();
    }


    public void setIncludedVertices(Set<Vertex> includedVertices) {
        for (Vertex vertex : includedVertices) {
            getNode().createRelationshipTo(
                    vertexFactory.withUri(vertex.uri()).getNode(),
                    Relationships.HAS_INCLUDED_VERTEX
            );
        }
    }

    public void setIncludedEdges(Set<Edge> includedEdges) {
        for (Edge edge : includedEdges) {
            getNode().createRelationshipTo(
                    edgeFactory.withUri(edge.uri()).getNode(),
                    Relationships.HAS_INCLUDED_EDGE
            );
        }
    }

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
    public void setSortDate(Date sortDate, Date moveDate) {
        graphElementOperator.setSortDate(
                sortDate,
                moveDate
        );
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
    public String getChildrenIndex() {
        return graphElementOperator.getChildrenIndex();
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
    public void mergeTo(VertexOperator vertexOperator) {
        this.connectedEdges().values().forEach(
                (edge) -> {
                    if (edge.destinationVertex().equals(this)) {
                        edge.changeDestinationVertex(vertexOperator);
                    } else {
                        edge.changeSourceVertex(vertexOperator);
                    }
                }
        );
        this.remove();
    }

    @Override
    public void setShareLevel(ShareLevel shareLevel) {
        ShareLevel previousShareLevel = this.getShareLevel();
        String decrementQueryPart = decrementNbFriendsOrPublicQueryPart(previousShareLevel, "d", "SET ");
        String incrementQueryPart = incrementNbFriendsOrPublicQueryPart(shareLevel, "d", "SET ");
        String query = queryPrefix()
                + "SET n.shareLevel=@shareLevel " +
                "WITH n " +
                "MATCH n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]->e, " +
                "e<-[:SOURCE_VERTEX|DESTINATION_VERTEX]->d " +
                decrementQueryPart + " " +
                incrementQueryPart + " " +
                "WITH d,n,e " +
                "SET e.shareLevel = CASE WHEN (n.shareLevel <= d.shareLevel) THEN n.shareLevel ELSE d.shareLevel END";
        NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection, query
            );
            statement.setInt("shareLevel", shareLevel.getConfidentialityIndex());
            return statement.execute();
        }).get();
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
        return NoEx.wrap(() -> {
            String query = String.format(
                    "create (n:%s {1})",
                    GraphElementType.vertex
            );
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(
                    1,
                    props
            );
            statement.execute();
            statement.close();
            return vertexPojo;
        }).get();
    }

    @Override
    public String queryPrefix() {
        return graphElementOperator.queryPrefix();
    }

    @Override
    public Node getNode() {
        if (null == node) {
            return graphElementOperator.getNode();
        }
        return node;
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Map<String, Object> newMap = map(
                props.shareLevel.name(), ShareLevel.PRIVATE.getConfidentialityIndex(),
                props.number_of_connected_edges_property_name.name(), 0,
                props.nb_public_neighbors.name(), 0,
                props.nb_friend_neighbors.name(), 0,
                FriendlyResourceNeo4j.props.type.name(), GraphElementType.vertex.name()
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

    @Override
    public void setNamedCreationProperties(NamedParameterStatement statement) throws SQLException {
        statement.setObject(
                props.is_public.name(),
                false
        );
        statement.setObject(
                props.number_of_connected_edges_property_name.name(),
                0
        );
        statement.setObject(
                props.number_of_connected_edges_property_name.name(),
                0
        );
        statement.setObject(
                FriendlyResourceNeo4j.props.type.name(),
                GraphElementType.vertex.name()
        );
        graphElementOperator.setNamedCreationProperties(
                statement
        );
    }
}
