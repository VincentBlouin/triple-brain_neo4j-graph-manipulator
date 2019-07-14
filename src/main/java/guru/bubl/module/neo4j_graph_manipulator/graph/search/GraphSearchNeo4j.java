/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphElementSearchResultPojo;
import guru.bubl.module.model.search.GraphSearch;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element.CenterGraphElementOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.GraphElementFromExtractorQueryRow;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.IdentificationNeo4j;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.QueryParser;

import javax.inject.Inject;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;

public class GraphSearchNeo4j implements GraphSearch {

    @Inject
    GraphElementFactoryNeo4j graphElementFactory;


    @Inject
    Connection connection;


    @Override
    public List<GraphElementSearchResult> searchForAllOwnResources(String searchTerm, User user) {
        return new Getter<>().get(
                searchTerm,
                true,
                user.username(),
                GraphElementType.vertex,
                GraphElementType.schema,
                GraphElementType.edge,
                GraphElementType.meta
        );
    }

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
        return new Getter<>().get(
                searchTerm,
                true,
                user.username(),
                GraphElementType.vertex
        );
    }

    @Override
    public List<GraphElementSearchResult> searchOwnTagsForAutoCompletionByLabel(String searchTerm, User user) {
        return new Getter<>().get(
                searchTerm,
                true,
                user.username(),
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
                    "START n=node:node_auto_index('uri:%s AND (shareLevel:40 %s)') " +
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

            return NoEx.wrap(() -> {
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
                    FriendlyResourceNeo4j.props.label + ":(" + formatSearchTerm(searchTerm) + "*) AND " +
                    (forPersonal ? "owner:" + username : "(shareLevel:40 " +
                            (StringUtils.isEmpty(username) ? "" : " OR owner:" + username) + ")") + " AND " +
                    "( " + FriendlyResourceNeo4j.props.type + ":" + StringUtils.join(graphElementTypes, " OR type:") + ") " +
                    "') " +
                    "OPTIONAL MATCH n-[idr:IDENTIFIED_TO]->id " +
                    "RETURN " +
                    "n.uri, n.label, n.external_uri, n.nb_references, n.number_of_visits, n.creation_date, n.last_modification_date, " +
                    "(CASE WHEN n.owner='" + username + "' THEN n.private_context ELSE n.public_context END) as context, " +
                    IdentificationQueryBuilder.identificationReturnQueryPart() +
                    "n.type as type " +
                    "ORDER BY COALESCE(" +
                    "n." + CenterGraphElementOperatorNeo4j.props.number_of_visits + "," +
                    "0) DESC, COALESCE(" +
                    "n." + IdentificationNeo4j.props.nb_references + "," +
                    "0) DESC " +
                    "limit 10";
        }
    }

    public static String formatSearchTerm(String searchTerm) {
        return QueryParser.escape(searchTerm).replace(
                "\\", "\\\\"
        ).replace("'", "\\'").replace(" ", " AND ");
    }
}
