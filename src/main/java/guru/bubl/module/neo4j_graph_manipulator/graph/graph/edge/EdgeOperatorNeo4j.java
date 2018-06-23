/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexInSubGraphOperatorNeo4j;
import org.neo4j.graphdb.Node;

import javax.inject.Inject;
import java.net.URI;
import java.sql.*;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.decrementNbFriendsOrPublicQueryPart;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementOperatorNeo4j.incrementNbFriendsOrPublicQueryPart;

public class EdgeOperatorNeo4j implements EdgeOperator, OperatorNeo4j {

    public enum props {
        source_vertex_uri,
        destination_vertex_uri
    }

    protected Node node;
    protected GraphElementOperatorNeo4j graphElementOperator;
    protected VertexFactoryNeo4j vertexFactory;
    protected EdgeFactoryNeo4j edgeFactory;

    protected Vertex sourceVertex;
    protected Vertex destinationVertex;

    @Inject
    protected
    Connection connection;

    @AssistedInject
    protected EdgeOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j graphElementFactory,
            @Assisted URI uri
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.graphElementOperator = graphElementFactory.withUri(
                uri
        );
    }

    @AssistedInject
    protected EdgeOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j graphElementFactory,
            @Assisted("source") Vertex sourceVertex,
            @Assisted("destination") Vertex destinationVertex
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        UserUris userUris = new UserUris(
                UserUris.ownerUserNameFromUri(sourceVertex.uri())
        );
        URI newEdgeUri = userUris.generateEdgeUri();
        this.graphElementOperator = graphElementFactory.withUri(
                newEdgeUri
        );
        this.sourceVertex = sourceVertex;
        this.destinationVertex = destinationVertex;
    }

    @AssistedInject
    protected EdgeOperatorNeo4j(
            VertexFactoryNeo4j vertexFactory,
            EdgeFactoryNeo4j edgeFactory,
            GraphElementFactoryNeo4j graphElementFactory,
            @Assisted URI uri,
            @Assisted("source") Vertex sourceVertex,
            @Assisted("destination") Vertex destinationVertex
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.graphElementOperator = graphElementFactory.withUri(
                uri
        );
        this.sourceVertex = sourceVertex;
        this.destinationVertex = destinationVertex;
    }

    @Override
    public VertexOperator sourceVertex() {
        return vertexUsingProperty(
                props.source_vertex_uri
        );
    }

    @Override
    public VertexOperator destinationVertex() {
        return vertexUsingProperty(
                props.destination_vertex_uri
        );
    }

    private VertexOperator vertexUsingProperty(Enum prop) {
        return NoEx.wrap(() -> {
            String query = String.format(
                    "%sRETURN n.%s",
                    queryPrefix(),
                    prop.name()
            );
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            rs.next();
            return vertexFactory.withUri(URI.create(
                    rs.getString(
                            "n." + prop.name()
                    )
            ));
        }).get();
    }

    @Override
    public VertexOperator otherVertex(Vertex vertex) {
        return sourceVertex().equals(vertex) ?
                destinationVertex() :
                sourceVertex();
    }

    @Override
    public void changeSourceVertex(
            Vertex newSourceVertex
    ) {
        changeEndVertex(
                newSourceVertex,
                Relationships.SOURCE_VERTEX
        );
    }

    @Override
    public void changeDestinationVertex(
            Vertex newDestinationVertex
    ) {
        changeEndVertex(
                newDestinationVertex,
                Relationships.DESTINATION_VERTEX
        );
    }

    private void changeEndVertex(
            Vertex newEndVertex,
            Relationships relationshipToChange
    ) {
        Relationships relationshipToKeep = Relationships.SOURCE_VERTEX == relationshipToChange ?
                Relationships.DESTINATION_VERTEX : Relationships.SOURCE_VERTEX;
        ShareLevel newEndVertexShareLevel = vertexFactory.withUri(
                newEndVertex.uri()
        ).getShareLevel();
        ShareLevel oldEndVertexShareLevel;
        ShareLevel keptEndVertexShareLevel;
        VertexOperator sourceVertex = sourceVertex();
        VertexOperator destinationVertex = destinationVertex();
        if (relationshipToChange == Relationships.SOURCE_VERTEX) {
            oldEndVertexShareLevel = sourceVertex.getShareLevel();
            keptEndVertexShareLevel = destinationVertex.getShareLevel();
        } else {
            oldEndVertexShareLevel = destinationVertex.getShareLevel();
            keptEndVertexShareLevel = sourceVertex.getShareLevel();
        }
        String decrementPreviousVertexQueryPart = keptEndVertexShareLevel == ShareLevel.PRIVATE ? "" :
                decrementNbFriendsOrPublicQueryPart(
                        keptEndVertexShareLevel,
                        "prev_v",
                        ", "
                );
        String incrementKeptVertexQueryPart = "";
        String decrementKeptVertexQueryPart = "";
        if (oldEndVertexShareLevel.isSame(keptEndVertexShareLevel)) {
            if (!newEndVertexShareLevel.isSame(keptEndVertexShareLevel)) {
                incrementKeptVertexQueryPart = incrementNbFriendsOrPublicQueryPart(
                        newEndVertexShareLevel,
                        "kept_v",
                        ", "
                );
            }
        } else {
            decrementKeptVertexQueryPart = decrementNbFriendsOrPublicQueryPart(
                    oldEndVertexShareLevel,
                    "kept_v",
                    ", "
            );
        }

        String incrementNewEndVertexQueryPart = incrementNbFriendsOrPublicQueryPart(
                keptEndVertexShareLevel,
                "new_v",
                ", "
        );
        String query = String.format(
                "%s, new_v=node:node_auto_index('%s:%s') " +
                        "MATCH n-[prev_rel:%s]->prev_v, " +
                        "n-[:%s]->kept_v " +
                        "CREATE (n)-[:%s]->(new_v) " +
                        "DELETE prev_rel " +
                        "SET n.%s=new_v.uri, " +
                        "prev_v.%s = prev_v.%s - 1, " +
                        "new_v.%s = new_v.%s + 1" +
                        decrementPreviousVertexQueryPart +
                        decrementKeptVertexQueryPart +
                        incrementKeptVertexQueryPart +
                        incrementNewEndVertexQueryPart,
                queryPrefix(),
                FriendlyResourceNeo4j.props.uri,
                newEndVertex.uri(),
                relationshipToChange,
                relationshipToKeep,
                relationshipToChange,
                Relationships.SOURCE_VERTEX == relationshipToChange ? props.source_vertex_uri : props.destination_vertex_uri,
                VertexInSubGraphOperatorNeo4j.props.number_of_connected_edges_property_name,
                VertexInSubGraphOperatorNeo4j.props.number_of_connected_edges_property_name,
                VertexInSubGraphOperatorNeo4j.props.number_of_connected_edges_property_name,
                VertexInSubGraphOperatorNeo4j.props.number_of_connected_edges_property_name

        );
        //todo batch
        NoEx.wrap(() ->
                connection.createStatement().executeQuery(
                        query
                )
        ).get();
        graphElementOperator.updateLastModificationDate();
        //todo endbatch
    }

    @Override
    public EdgeOperator forkUsingSourceAndDestinationVertexAndCache(
            Vertex sourceVertex,
            Vertex destinationVertex,
            Edge cache
    ) {
        EdgeOperator clone = edgeFactory.withSourceAndDestinationVertex(
                sourceVertex,
                destinationVertex
        );
        graphElementOperator.forkUsingCreationPropertiesAndCache(
                clone,
                map(),
                cache
        );
        return clone;
    }


    @Override
    public void inverse() {
        String query = String.format(
                "%sMATCH n-[source_rel:%s]->source_vertex, " +
                        "n-[destination_rel:%s]->destination_vertex " +
                        "CREATE (n)-[:%s]->(source_vertex) " +
                        "CREATE (n)-[:%s]->(destination_vertex) " +
                        "DELETE source_rel, destination_rel " +
                        "SET n.%s=destination_vertex.uri, n.%s=source_vertex.uri",
                queryPrefix(),
                Relationships.SOURCE_VERTEX,
                Relationships.DESTINATION_VERTEX,
                Relationships.DESTINATION_VERTEX,
                Relationships.SOURCE_VERTEX,
                props.source_vertex_uri,
                props.destination_vertex_uri
        );
        //todo batch
        NoEx.wrap(() ->
                connection.createStatement().executeQuery(
                        query
                )
        ).get();
        graphElementOperator.updateLastModificationDate();
        //todo endbatch

    }

    @Override
    public void remove() {
        //todo batch
        NoEx.wrap(() -> {
            ShareLevel sourceVertexShareLevel = sourceVertex().getShareLevel();
            ShareLevel destinationVertexShareLevel = destinationVertex().getShareLevel();
            Statement statement = connection.createStatement();
            statement.executeQuery(
                    String.format(
                            "%s" +
                                    "MATCH n-[:SOURCE_VERTEX]->(s_v), n-[:DESTINATION_VERTEX]->(d_v) " +
                                    "SET s_v.number_of_connected_edges_property_name = s_v.number_of_connected_edges_property_name - 1, " +
                                    "d_v.number_of_connected_edges_property_name = d_v.number_of_connected_edges_property_name - 1" +
                                    (sourceVertexShareLevel == ShareLevel.PRIVATE ? "" : decrementNbFriendsOrPublicQueryPart(sourceVertexShareLevel, "d_v", ", ")) +
                                    (destinationVertexShareLevel == ShareLevel.PRIVATE ? "" : decrementNbFriendsOrPublicQueryPart(destinationVertexShareLevel, "s_v", ", ")),
                            queryPrefix()
                    )
            );
            graphElementOperator.removeAllIdentifications();
            return connection.createStatement().executeQuery(
                    String.format(
                            "%sMATCH n-[r]-() DELETE r, n",
                            queryPrefix()
                    )
            );
        }).get();
        //todo endbatch
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
    public URI uri() {
        return graphElementOperator.uri();
    }

    @Override
    public String label() {
        return graphElementOperator.label();
    }

    @Override
    public void label(String label) {
        graphElementOperator.label(label);
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
        graphElementOperator.addImages(images);
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
    public void create() {
        createEdge();
    }

    @Override
    public EdgePojo createEdge() {
        return createEdgeUsingInitialValues(
                map(
                        VertexInSubGraphOperatorNeo4j.props.shareLevel.name(), ShareLevel.PRIVATE.getConfidentialityIndex()
                )
        );
    }

    @Override
    public EdgePojo createWithShareLevel(ShareLevel shareLevel) {
        return createEdgeUsingInitialValues(
                map(
                        VertexInSubGraphOperatorNeo4j.props.shareLevel.name(),
                        shareLevel.getConfidentialityIndex()
                )
        );
    }

    @Override
    public EdgePojo createEdgeUsingInitialValues(Map<String, Object> values) {
        String query = String.format(
                "START source_node=node:node_auto_index(\"uri:%s\"), " +
                        "destination_node=node:node_auto_index(\"uri:%s\") " +
                        "CREATE (n:%s {1}), n-[:%s]->source_node, n-[:%s]->destination_node ",
                sourceVertex.uri(),
                destinationVertex.uri(),
                GraphElementType.edge,
                Relationships.SOURCE_VERTEX.name(),
                Relationships.DESTINATION_VERTEX.name()
        );
        return NoEx.wrap(() -> {
            PreparedStatement statement = connection.prepareStatement(
                    query
            );
            Map<String, Object> creationProperties = addCreationProperties(values);
            statement.setObject(
                    1,
                    creationProperties
            );
            statement.execute();
            statement.close();
            EdgePojo edge = new EdgePojo(
                    graphElementOperator.pojoFromCreationProperties(
                            creationProperties
                    )
            );
            edge.setSourceVertex(new VertexInSubGraphPojo(
                    sourceVertex.uri()
            ));
            edge.setDestinationVertex(new VertexInSubGraphPojo(
                    destinationVertex.uri()
            ));
            return edge;
        }).get();
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        createEdgeUsingInitialValues(values);
    }

    @Override
    public void removeIdentification(Identifier type) {
        graphElementOperator.removeIdentification(type);
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
    public boolean equals(Object edgeToCompareAsObject) {
        return graphElementOperator.equals(edgeToCompareAsObject);
    }

    @Override
    public int hashCode() {
        return graphElementOperator.hashCode();
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
                props.source_vertex_uri.name(), sourceVertex.uri().toString(),
                props.destination_vertex_uri.name(), destinationVertex.uri().toString(),
                FriendlyResourceNeo4j.props.type.name(), GraphElementType.edge.name(),
                VertexInSubGraphOperatorNeo4j.props.shareLevel.name(), ShareLevel.PRIVATE.getConfidentialityIndex()
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
        statement.setString(
                props.source_vertex_uri.name(),
                sourceVertex.uri().toString()
        );
        statement.setString(
                props.destination_vertex_uri.name(),
                destinationVertex.uri().toString()
        );
        statement.setString(
                FriendlyResourceNeo4j.props.type.name(),
                GraphElementType.edge.name()
        );
        graphElementOperator.setNamedCreationProperties(statement);
    }

    @Override
    public Boolean isPublic() {
        return graphElementOperator.isPublic();
    }

    @Override
    public ShareLevel getShareLevel() {
        return graphElementOperator.getShareLevel();
    }
}
