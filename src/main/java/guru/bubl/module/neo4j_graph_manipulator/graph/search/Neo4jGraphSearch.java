/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphElementSearchResultPojo;
import guru.bubl.module.model.search.GraphSearch;
import guru.bubl.module.model.search.VertexSearchResult;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.GraphElementFromExtractorQueryRow;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder.*;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.QueryParser;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.ReadableIndex;

import javax.inject.Inject;
import javax.xml.transform.Result;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Neo4jGraphSearch implements GraphSearch {

    @Inject
    GraphDatabaseService graphDatabaseService;

    @Inject
    Neo4jGraphElementFactory graphElementFactory;

    @Inject
    ReadableIndex<Node> nodeIndex;

    @Inject
    Connection connection;


    @Override
    public List<GraphElementSearchResult> searchForAnyResourceThatCanBeUsedAsAnIdentifier(String searchTerm, User user) {
        return new Getter<GraphElementSearchResult>().get(
                searchTerm,
                false,
                user.username(),
                GraphElementType.vertex.name(),
                GraphElementType.schema.name(),
                IdentificationType.generic.name(),
                IdentificationType.type.name(),
                IdentificationType.same_as.name()
        );
    }

    @Override
    public List<VertexSearchResult> searchOnlyForOwnVerticesOrSchemasForAutoCompletionByLabel(String searchTerm, User user) {
        return new Getter<VertexSearchResult>().get(
                searchTerm,
                true,
                user.username(),
                GraphElementType.vertex.name(),
                GraphElementType.schema.name()
        );
    }

    @Override
    public List<VertexSearchResult> searchOnlyForOwnVerticesForAutoCompletionByLabel(String searchTerm, User user) {
        return new Getter<VertexSearchResult>().get(
                searchTerm,
                true,
                user.username(),
                GraphElementType.vertex.name()
        );
    }

    @Override
    public List<GraphElementSearchResult> searchRelationsPropertiesSchemasOrIdentifiersForAutoCompletionByLabel(String searchTerm, User user) {
        return new Getter<GraphElementSearchResult>().get(
                searchTerm,
                false,
                user.username(),
                GraphElementType.schema.name(),
                GraphElementType.property.name(),
                GraphElementType.edge.name(),
                IdentificationType.generic.name(),
                IdentificationType.type.name(),
                IdentificationType.same_as.name()
        );
    }

    @Override
    public GraphElementSearchResult getDetails(URI uri, User user) {
        return new Getter().getForUri(
                uri,
                user.username()
        );
    }

    @Override
    public List<VertexSearchResult> searchPublicVerticesOnly(String searchTerm) {
        return new Getter<VertexSearchResult>().get(
                searchTerm,
                false,
                "",
                GraphElementType.vertex.name(),
                GraphElementType.schema.name()
        );
    }

    @Override
    public GraphElementSearchResult getDetailsAnonymously(URI uri) {
        return new Getter().getForUri(
                uri,
                ""
        );
    }

    private class Getter<ResultType extends GraphElementSearchResult> {

        public GraphElementSearchResult getForUri(URI uri, String username) {
            String query = String.format(
                    "START node=node:node_auto_index('uri:%s AND (is_public:true %s)') " +
                            "OPTIONAL MATCH (node)-[%s:%s]->(%s) " +
                            "RETURN %s%s%snode.type as type",
                    uri,
                    StringUtils.isEmpty(username) ? "" : " OR owner:" + username,
                    IdentificationQueryBuilder.IDENTIFICATION_RELATION_QUERY_KEY,
                    Relationships.IDENTIFIED_TO,
                    IdentificationQueryBuilder.IDENTIFICATION_QUERY_KEY,
                    FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix("node"),
                    FriendlyResourceQueryBuilder.imageReturnQueryPart("node"),
                    IdentificationQueryBuilder.identificationReturnQueryPart()
            );

            return NoExRun.wrap(() -> {
                ResultSet rs = connection.createStatement().executeQuery(query);
                if (!rs.next()) {
                    return null;
                }
                return new GraphElementSearchResultPojo(
                        setupGraphElementForDetailedResult(
                                GraphElementFromExtractorQueryRow.usingRowAndKey(
                                        rs,
                                        "node"
                                ).build()
                        ),
                        SearchResultGetter.nodeTypeInRow(rs)
                );
            }).get();
        }


        private GraphElementPojo setupGraphElementForDetailedResult(GraphElementPojo graphElement) {
            if (graphElement.gotImages()) {
                graphElement.removeAllIdentifications();
                return graphElement;
            }
            for (IdentificationPojo identification : graphElement.getIdentifications().values()) {
                if (identification.gotImages()) {
                    graphElement.addImage(
                            identification.images().iterator().next()
                    );
                    graphElement.removeAllIdentifications();
                    return graphElement;
                }
            }
            graphElement.removeAllIdentifications();
            return graphElement;
        }

        public List<ResultType> get(
                String searchTerm,
                Boolean forPersonal,
                String username,
                String... graphElementTypes
        ) {
            return new SearchResultGetter<ResultType>(
                    buildQuery(
                            searchTerm,
                            forPersonal,
                            username,
                            graphElementTypes
                    ),
                    connection
            ).get();
        }

        private String buildQuery(
                String searchTerm,
                Boolean forPersonal,
                String username,
                String... graphElementTypes
        ) {
            return "START node=node:node_auto_index('" +
                    Neo4jFriendlyResource.props.label + ":(" + formatSearchTerm(searchTerm) + "*) AND " +
                    (forPersonal ? "owner:" + username : "(is_public:true " +
                            (StringUtils.isEmpty(username) ? "" : " OR owner:" + username) + ")") + " AND " +
                    "( " + Neo4jFriendlyResource.props.type + ":" + StringUtils.join(graphElementTypes, " OR type:") + ") " +
                    "') " +
                    "OPTIONAL MATCH node<-[relation]->related_node " +
                    "WHERE related_node." +
                    Neo4jVertexInSubGraphOperator.props.is_public +
                    "=true OR related_node.owner='" + username + "' " +
                    "RETURN " +
                    "node.uri, node.label, node.creation_date, node.last_modification_date, node.nb_references, " +
                    "COLLECT([related_node.label, related_node.uri, type(relation)])[0..5] as related_nodes, " +
                    "node.type as type limit 10";
        }

        private String formatSearchTerm(String searchTerm) {
            return QueryParser.escape(searchTerm).replace(
                    "\\", "\\\\"
            ).replace("'", "\\'").replace(" ", " AND ");
        }
    }
}
