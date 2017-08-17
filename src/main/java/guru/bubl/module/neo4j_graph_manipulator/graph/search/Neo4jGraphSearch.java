/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphElementSearchResultPojo;
import guru.bubl.module.model.search.GraphSearch;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element.Neo4jCenterGraphElementOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.GraphElementFromExtractorQueryRow;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.Neo4jIdentification;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.QueryParser;

import javax.inject.Inject;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;

public class Neo4jGraphSearch implements GraphSearch {

    @Inject
    Neo4jGraphElementFactory graphElementFactory;


    @Inject
    Connection connection;


    @Override
    public List<GraphElementSearchResult> searchForAnyResourceThatCanBeUsedAsAnIdentifier(String searchTerm, User user) {
        return new Getter<>().get(
                searchTerm,
                false,
                user.username(),
                GraphElementType.vertex,
                GraphElementType.schema,
                GraphElementType.meta
        );
    }

    @Override
    public List<GraphElementSearchResult> searchOnlyForOwnVerticesOrSchemasForAutoCompletionByLabel(String searchTerm, User user) {
        return new Getter<>().get(
                searchTerm,
                true,
                user.username(),
                GraphElementType.vertex,
                GraphElementType.schema,
                GraphElementType.meta
        );
    }

    @Override
    public List<GraphElementSearchResult> searchOnlyForOwnVerticesForAutoCompletionByLabel(String searchTerm, User user) {
        return new Getter<GraphElementSearchResult>().get(
                searchTerm,
                true,
                user.username(),
                GraphElementType.vertex,
                GraphElementType.meta
        );
    }

    @Override
    public List<GraphElementSearchResult> searchRelationsPropertiesSchemasForAutoCompletionByLabel(String searchTerm, User user) {
        return new Getter<GraphElementSearchResult>().get(
                searchTerm,
                false,
                user.username(),
                GraphElementType.schema,
                GraphElementType.property,
                GraphElementType.edge,
                GraphElementType.meta
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
    public List<GraphElementSearchResult> searchPublicVerticesOnly(String searchTerm) {
        return new Getter<GraphElementSearchResult>().get(
                searchTerm,
                false,
                "",
                GraphElementType.vertex,
                GraphElementType.schema,
                GraphElementType.meta
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
                    "START n=node:node_auto_index('uri:%s AND (is_public:true %s)') " +
                            "OPTIONAL MATCH (n)-[%s:%s]->(%s) " +
                            "RETURN %s%s%sn.type as type",
                    uri,
                    StringUtils.isEmpty(username) ? "" : " OR owner:" + username,
                    IdentificationQueryBuilder.IDENTIFICATION_RELATION_QUERY_KEY,
                    Relationships.IDENTIFIED_TO,
                    IdentificationQueryBuilder.IDENTIFIER_QUERY_KEY,
                    FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix("n"),
                    FriendlyResourceQueryBuilder.imageReturnQueryPart("n"),
                    IdentificationQueryBuilder.identificationReturnQueryPart()
            );

            return NoExRun.wrap(() -> {
                ResultSet rs = connection.createStatement().executeQuery(query);
                if (!rs.next()) {
                    return null;
                }
                return new GraphElementSearchResultPojo(
                        SearchResultGetter.nodeTypeInRow(rs),
                        setupGraphElementForDetailedResult(
                                GraphElementFromExtractorQueryRow.usingRowAndKey(
                                        rs,
                                        "n"
                                ).build()
                        ),
                        new HashMap<>()
                );
            }).get();
        }


        private GraphElementPojo setupGraphElementForDetailedResult(GraphElementPojo graphElement) {
            if (graphElement.gotImages()) {
                graphElement.removeAllIdentifications();
                return graphElement;
            }
            for (IdentifierPojo identification : graphElement.getIdentifications().values()) {
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
                GraphElementType... graphElementTypes
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
                GraphElementType... graphElementTypes
        ) {
            return "START n=node:node_auto_index('" +
                    Neo4jFriendlyResource.props.label + ":(" + formatSearchTerm(searchTerm) + "*) AND " +
                    (forPersonal ? "owner:" + username : "(is_public:true " +
                            (StringUtils.isEmpty(username) ? "" : " OR owner:" + username) + ")") + " AND " +
                    "( " + Neo4jFriendlyResource.props.type + ":" + StringUtils.join(graphElementTypes, " OR type:") + ") " +
                    "') " +
                    "OPTIONAL MATCH n-[idr:IDENTIFIED_TO]->id " +
                    "RETURN " +
                    "n.uri, n.label, n.external_uri, n.nb_references, n.number_of_visits, n.creation_date, n.last_modification_date, " +
                    "(CASE WHEN n.owner='" + username + "' THEN n.private_context ELSE n.public_context END) as context, " +
                    IdentificationQueryBuilder.identificationReturnQueryPart() +
                    "n.type as type " +
                    "ORDER BY COALESCE(" +
                    "n." + Neo4jIdentification.props.nb_references + "," +
                    "0) DESC, COALESCE(" +
                    "n." + Neo4jCenterGraphElementOperator.props.number_of_visits + "," +
                    "0) DESC " +
                    "limit 10";
        }

        private String formatSearchTerm(String searchTerm) {
            return QueryParser.escape(searchTerm).replace(
                    "\\", "\\\\"
            ).replace("'", "\\'").replace(" ", " AND ");
        }
    }
}
