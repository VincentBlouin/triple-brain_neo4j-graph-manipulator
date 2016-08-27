/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.User;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.identification.Identification;
import guru.bubl.module.model.graph.identification.IdentificationPojo;
import guru.bubl.module.model.graph.vertex.*;
import guru.bubl.module.model.json.SuggestionJson;
import guru.bubl.module.model.suggestion.SuggestionPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResourceFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.Neo4jSubGraphExtractor;
import org.neo4j.graphdb.Node;

import java.net.URI;
import java.sql.*;
import java.util.Date;
import java.util.*;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class Neo4jVertexInSubGraphOperator implements VertexInSubGraphOperator, Neo4jOperator {

    public enum props {
        number_of_connected_edges_property_name,
        nb_public_neighbors,
        is_public,
        suggestions
    }

    protected Neo4jGraphElementOperator graphElementOperator;
    protected Neo4jVertexFactory vertexFactory;

    protected Neo4jEdgeFactory edgeFactory;

    protected Neo4jGraphElementFactory neo4jGraphElementFactory;
    protected Node node;
    protected Connection connection;


    @Inject
    protected Neo4jFriendlyResourceFactory friendlyResourceFactory;

    @AssistedInject
    protected Neo4jVertexInSubGraphOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory neo4jGraphElementFactory,
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
    protected Neo4jVertexInSubGraphOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory neo4jGraphElementFactory,
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
    protected Neo4jVertexInSubGraphOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory neo4jGraphElementFactory,
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
    protected Neo4jVertexInSubGraphOperator(
            final Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jGraphElementFactory neo4jGraphElementFactory,
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
        Neo4jFriendlyResource edgeFriendlyResource = friendlyResourceFactory.withUri(
                edge.uri()
        );
        String query = String.format(
                "%s, %sMATCH n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-edge RETURN edge",
                queryPrefix(),
                edgeFriendlyResource.addToSelectUsingVariableName("edge")
        );
        return NoExRun.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            return rs.next();
        }).get();
    }

    @Override
    public EdgeOperator getEdgeThatLinksToDestinationVertex(Vertex destinationVertex) {
        Neo4jFriendlyResource destinationVertexOperator = friendlyResourceFactory.withUri(
                destinationVertex.uri()
        );
        return NoExRun.wrap(() -> {
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
        Neo4jFriendlyResource destinationVertexOperator = friendlyResourceFactory.withUri(
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

        return NoExRun.wrap(() -> {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            return rs.next();
        }).get();
    }

    @Override
    public EdgePojo addVertexAndRelation() {
        UserUris userUris = new UserUris(
                getOwnerUsername()
        );
        return addVertexAndRelationAction(
                this,
                userUris.generateVertexUri()
        );
    }

    private EdgePojo addVertexAndRelationAction(Neo4jVertexInSubGraphOperator self, URI newVertexUri) {
        Neo4jVertexInSubGraphOperator newVertexOperator = vertexFactory.withUri(
                newVertexUri
        );
        self.incrementNumberOfConnectedEdges();
        VertexPojo newVertex = newVertexOperator.createVertexUsingInitialValues(
                map(
                        props.number_of_connected_edges_property_name.name(),
                        1,
                        props.nb_public_neighbors.name(),
                        self.isPublic() ? 1 : 0
                )
        );
        Neo4jEdgeOperator edgeOperator = edgeFactory.withSourceAndDestinationVertex(
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
    public EdgeOperator addRelationToVertex(final Vertex destinationVertex) {
        //todo batch
        EdgeOperator edge = edgeFactory.withSourceAndDestinationVertex(
                this,
                destinationVertex
        );
        edge.create();
        String query = String.format(
                "MATCH (s:%s {uri:'%s'}), (d:%s {uri:'%s'}) " +
                        "SET " +
                        "s.%s=s.%s+1, " +
                        "d.%s=d.%s+1, " +
                        "s.%s = CASE WHEN d.%s THEN s.%s + 1 ELSE s.%s END, " +
                        "d.%s = CASE WHEN s.%s THEN d.%s + 1 ELSE d.%s END ",
                GraphElementType.vertex,
                this.uri(),
                GraphElementType.vertex,
                destinationVertex.uri(),
                props.number_of_connected_edges_property_name,
                props.number_of_connected_edges_property_name,
                props.number_of_connected_edges_property_name,
                props.number_of_connected_edges_property_name,
                props.nb_public_neighbors,
                props.is_public,
                props.nb_public_neighbors,
                props.nb_public_neighbors,
                props.nb_public_neighbors,
                props.is_public,
                props.nb_public_neighbors,
                props.nb_public_neighbors
        );
        NoExRun.wrap(() -> connection.createStatement().executeQuery(
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
        Edge newEdge = addVertexAndRelationAction(
                this,
                newVertex.uri()
        );
        EdgeOperator newEdgeOperator = edgeFactory.withUri(
                newEdge.uri()
        );
        newEdgeOperator.label(
                suggestion.label()
        );

        if (suggestion.getSameAs() != null) {
            newEdgeOperator.addSameAs(
                    new IdentificationPojo(
                            suggestion.getSameAs().uri(),
                            suggestion.getSameAs()
                    )
            );
            newVertex.addType(
                    new IdentificationPojo(
                            suggestion.getSameAs().uri(),
                            suggestion.getSameAs()
                    )
            );
        }
        if (suggestion.getType() != null) {
            newVertex.addType(
                    new IdentificationPojo(
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
        EdgePojo newEdge = addVertexAndRelationAction(
                this,
                newVertex.uri()
        );
        EdgeOperator newEdgeOperator = edgeFactory.withUri(
                newEdge.uri()
        );
        newEdgeOperator.label(
                suggestion.label()
        );
        newEdgeOperator.addGenericIdentification(
                new IdentificationPojo(
                        suggestion.getSameAs().uri(),
                        suggestion.getSameAs()
                )
        );
        newVertex.addGenericIdentification(
                new IdentificationPojo(
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
        NoExRun.wrap(() -> {
            graphElementOperator.removeAllIdentifications();
            return connection.createStatement().executeQuery(
                    queryPrefix() +
                            "OPTIONAL MATCH " +
                            "n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(e), " +
                            "e-[:SOURCE_VERTEX|DESTINATION_VERTEX]->(v) " +
                            "SET " +
                            "v.number_of_connected_edges_property_name = " +
                            "v.number_of_connected_edges_property_name - 1, " +
                            "v.nb_public_neighbors = CASE WHEN n.is_public THEN v.nb_public_neighbors - 1 ELSE v.nb_public_neighbors END " +
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
    public Set<EdgeOperator> connectedEdges() {
        Set<EdgeOperator> edges = new HashSet<>();
        String query = queryPrefix() +
                "MATCH n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(edge) " +
                "RETURN edge.uri as uri";
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            while (rs.next()) {
                edges.add(edgeFactory.withUri(
                        URI.create(
                                rs.getString(
                                        "uri"
                                )
                        )
                ));
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
        return NoExRun.wrap(() -> {
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
        return NoExRun.wrap(() -> {
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
        NoExRun.wrap(() -> {
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

    protected void incrementNumberOfConnectedEdges() {
        NoExRun.wrap(() -> {
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
        NoExRun.wrap(() -> {
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
        return NoExRun.wrap(() -> {
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
    public Map<URI, IdentificationPojo> addType(Identification type) {
        return graphElementOperator.addType(type);
    }

    @Override
    public void removeIdentification(Identification identification) {
        graphElementOperator.removeIdentification(identification);
    }

    @Override
    public Map<URI, IdentificationPojo> getAdditionalTypes() {
        return graphElementOperator.getAdditionalTypes();
    }

    @Override
    public Map<URI, IdentificationPojo> getIdentifications() {
        return graphElementOperator.getIdentifications();
    }

    @Override
    public Map<URI, IdentificationPojo> addSameAs(Identification friendlyResourceImpl) {
        return graphElementOperator.addSameAs(friendlyResourceImpl);
    }

    @Override
    public Map<URI, IdentificationPojo> getSameAs() {
        return graphElementOperator.getSameAs();
    }

    @Override
    public Map<URI, IdentificationPojo> getGenericIdentifications() {
        return graphElementOperator.getGenericIdentifications();
    }

    @Override
    public Boolean isPublic() {
        String query = String.format(
                "%s return n.%s as is_public",
                queryPrefix(),
                props.is_public
        );
        return NoExRun.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            return rs.getBoolean("is_public");
        }).get();
    }

    @Override
    public void makePublic() {
        String query = String.format(
                "%s" +
                        "SET n.%s = true " +
                        "WITH n " +
                        "MATCH n<-[:%s|%s]->e, " +
                        "e<-[:%s|%s]->d " +
                        "SET d.%s = " +
                        "d.%s + 1 " +
                        "WITH d,e " +
                        "WHERE d.%s = true " +
                        "SET e.%s = true",
                queryPrefix(),
                props.is_public,
                Relationships.SOURCE_VERTEX,
                Relationships.DESTINATION_VERTEX,
                Relationships.SOURCE_VERTEX,
                Relationships.DESTINATION_VERTEX,
                props.nb_public_neighbors,
                props.nb_public_neighbors,
                props.is_public,
                props.is_public
        );
        NoExRun.wrap(() ->
                connection.createStatement().executeQuery(
                        query
                )).get();
        graphElementOperator.updateLastModificationDate();
    }

    @Override
    public void makePrivate() {
        String query = String.format(
                "%s" +
                        "WITH n " +
                        "MATCH n<-[:%s|%s]->e, " +
                        "e<-[:%s|%s]->d " +
                        "SET n.%s = false, " +
                        "e.%s = false, " +
                        "d.%s = " +
                        "d.%s -1 ",
                queryPrefix(),
                Relationships.SOURCE_VERTEX,
                Relationships.DESTINATION_VERTEX,
                Relationships.SOURCE_VERTEX,
                Relationships.DESTINATION_VERTEX,
                props.is_public,
                props.is_public,
                props.nb_public_neighbors,
                props.nb_public_neighbors
        );
        NoExRun.wrap(() ->
                connection.createStatement().executeQuery(
                        query
                )).get();
        graphElementOperator.updateLastModificationDate();
    }

    @Override
    public Map<URI, Vertex> getIncludedVertices() {
        String query = String.format(
                "%sMATCH n-[:%s]->included_vertex RETURN %s'dummy_return'",
                queryPrefix(),
                Relationships.HAS_INCLUDED_VERTEX,
                Neo4jSubGraphExtractor.includedElementQueryPart("included_vertex")
        );
        return NoExRun.wrap(() -> {
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
                                        "included_vertex." + Neo4jFriendlyResource.props.label.toString()
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
                Neo4jSubGraphExtractor.includedElementQueryPart("included_edge")
        );
        return NoExRun.wrap(() -> {
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
                                        "included_edge." + Neo4jFriendlyResource.props.label.toString()
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
    public Map<URI, IdentificationPojo> addGenericIdentification(Identification friendlyResource) {
        return graphElementOperator.addGenericIdentification(friendlyResource);
    }

    @Override
    public void setSortDate(Date sortDate, Date moveDate) {
        graphElementOperator.setSortDate(
                sortDate,
                moveDate
        );
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

    public VertexPojo createVertexUsingInitialValues(Map<String, Object> values) {
        Map<String, Object> props = addCreationProperties(
                values
        );
        VertexPojo vertexPojo = pojoFromCreationProperties(
                props
        );
        NoExRun.wrap(() -> {
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
            return null;
        }).get();
        return vertexPojo;
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
                props.is_public.name(), false,
                props.number_of_connected_edges_property_name.name(), 0,
                props.nb_public_neighbors.name(), 0,
                Neo4jFriendlyResource.props.type.name(), GraphElementType.vertex.name()
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
                Neo4jFriendlyResource.props.type.name(),
                GraphElementType.vertex.name()
        );
        graphElementOperator.setNamedCreationProperties(
                statement
        );
    }
}
