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
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.Identification;
import guru.bubl.module.model.graph.IdentificationPojo;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphOperator;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.model.graph.vertex.VertexOperator;
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

    private final static Integer DISTANCE_TO_GET_END_VERTICES = 2;

    public enum props {
        number_of_connected_edges_property_name,
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
    public EdgeOperator addVertexAndRelation() {
        UserUris userUris = new UserUris(
                getOwnerUsername()
        );
        return addVertexAndRelationAction(
                this,
                userUris.generateVertexUri()
        );
    }

    private EdgeOperator addVertexAndRelationAction(Neo4jVertexInSubGraphOperator self, URI newVertexUri) {
        Neo4jVertexInSubGraphOperator newVertexOperator = vertexFactory.withUri(
                newVertexUri
        );
        self.incrementNumberOfConnectedEdges();
        newVertexOperator.createUsingInitialValues(
                map(
                        props.number_of_connected_edges_property_name.name(),
                        1
                )
        );
        Neo4jEdgeOperator edgeOperator = edgeFactory.withSourceAndDestinationVertex(
                self,
                newVertexOperator
        );
        edgeOperator.create();
        return edgeOperator;
    }

    @Override
    public EdgeOperator addRelationToVertex(final Vertex destinationVertex) {
        //todo batch
        incrementNumberOfConnectedEdges();
        (
                (Neo4jVertexInSubGraphOperator) destinationVertex
        ).incrementNumberOfConnectedEdges();
        EdgeOperator edge = edgeFactory.withSourceAndDestinationVertex(
                this,
                destinationVertex
        );
        edge.create();
        return edge;
        //todo endbatch
    }

    @Override
    public EdgeOperator acceptSuggestion(final SuggestionPojo suggestion) {
        //todo batch
        UserUris userUris = new UserUris(
                getOwnerUsername()
        );
        URI destinationVertexUri = userUris.generateVertexUri();
        EdgeOperator newEdge = addVertexAndRelationAction(
                this,
                destinationVertexUri
        );
        newEdge.label(
                suggestion.label()
        );
        VertexOperator newVertex = vertexFactory.withUri(
                destinationVertexUri
        );
        if (suggestion.getSameAs() != null) {
            newEdge.addSameAs(
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
        return newEdge;
        //todo endbatch
    }

    @Override
    public void remove() {
        //todo batch
        NoExRun.wrap(() -> {
            connection.createStatement().executeQuery(
                    queryPrefix() +
                            "MATCH " +
                            "n<-[:SOURCE_VERTEX]-(edge), " +
                            "edge-[:DESTINATION_VERTEX]->(vertex) " +
                            "SET " +
                            "vertex.number_of_connected_edges_property_name = " +
                            "vertex.number_of_connected_edges_property_name - 1"
            );
            connection.createStatement().executeQuery(
                    queryPrefix() +
                            "MATCH " +
                            "n<-[:DESTINATION_VERTEX]-(edge), " +
                            "edge-[:SOURCE_VERTEX]->(vertex) " +
                            "SET " +
                            "vertex.number_of_connected_edges_property_name = " +
                            "vertex.number_of_connected_edges_property_name - 1"
            );
            return connection.createStatement().executeQuery(
                    queryPrefix() +
                            "OPTIONAL MATCH " +
                            "n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(edge), " +
                            "edge-[edge_relation]-() " +
                            "OPTIONAL MATCH " +
                            "n-[vertex_relation]-() " +
                            "DELETE " +
                            "vertex_relation, n, " +
                            "edge_relation, edge"
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
            return statement.executeUpdate(
                    query
            );
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
    public IdentificationPojo addType(Identification type) {
        return graphElementOperator.addType(type);
    }

    @Override
    public void removeIdentification(Identification identification) {
        graphElementOperator.removeIdentification(identification);
    }

    @Override
    public Map<URI, Identification> getAdditionalTypes() {
        return graphElementOperator.getAdditionalTypes();
    }

    @Override
    public Map<URI, IdentificationPojo> getIdentifications() {
        return graphElementOperator.getIdentifications();
    }

    @Override
    public IdentificationPojo addSameAs(Identification friendlyResourceImpl) {
        return graphElementOperator.addSameAs(friendlyResourceImpl);
    }

    @Override
    public Map<URI, Identification> getSameAs() {
        return graphElementOperator.getSameAs();
    }

    @Override
    public Map<URI, Identification> getGenericIdentifications() {
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
                        "e<-[:%s|%s]->v " +
                        "WHERE v.%s = true " +
                        "SET e.%s = true ",
                queryPrefix(),
                props.is_public,
                Relationships.SOURCE_VERTEX,
                Relationships.DESTINATION_VERTEX,
                Relationships.SOURCE_VERTEX,
                Relationships.DESTINATION_VERTEX,
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
                        "OPTIONAL MATCH n<-[:%s|%s]->e " +
                        "SET n.%s = false, " +
                        "e.%s = false ",
                queryPrefix(),
                Relationships.SOURCE_VERTEX,
                Relationships.DESTINATION_VERTEX,
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
    public IdentificationPojo addGenericIdentification(Identification friendlyResource) {

        return graphElementOperator.addGenericIdentification(friendlyResource);
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
        Map<String, Object> props = addCreationProperties(
                values
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
            return statement.execute();
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
                props.is_public.name(), false,
                props.number_of_connected_edges_property_name.name(), 0,
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

    @Override
    public URI getExternalResourceUri() {
        return graphElementOperator.getExternalResourceUri();
    }
}
