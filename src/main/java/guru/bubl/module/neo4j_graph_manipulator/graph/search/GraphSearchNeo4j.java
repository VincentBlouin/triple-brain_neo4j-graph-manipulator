/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphElementSearchResultPojo;
import guru.bubl.module.model.search.GraphSearch;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.GraphElementFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.GraphElementFromExtractorQueryRow;
import org.apache.commons.lang.StringUtils;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import javax.inject.Inject;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import static org.neo4j.driver.v1.Values.parameters;

public class GraphSearchNeo4j implements GraphSearch {

    @Inject
    GraphElementFactoryNeo4j graphElementFactory;


    @Inject
    Driver driver;


    @Override
    public List<GraphElementSearchResult> searchForAllOwnResources(String searchTerm, User user) {
        return new Getter<>().get(
                searchTerm,
                true,
                user.username(),
                GraphElementType.Vertex,
                GraphElementType.Schema,
                GraphElementType.Edge,
                GraphElementType.Meta
        );
    }

    @Override
    public List<GraphElementSearchResult> searchForAnyResourceThatCanBeUsedAsAnIdentifier(String searchTerm, User user) {
        return new Getter<>().get(
                searchTerm,
                false,
                user.username(),
                GraphElementType.Vertex,
                GraphElementType.Schema,
                GraphElementType.Meta,
                GraphElementType.Edge
        );
    }

    @Override
    public List<GraphElementSearchResult> searchOnlyForOwnVerticesOrSchemasForAutoCompletionByLabel(String searchTerm, User user) {
        return new Getter<>().get(
                searchTerm,
                true,
                user.username(),
                GraphElementType.Vertex,
                GraphElementType.Schema,
                GraphElementType.Meta
        );
    }

    @Override
    public List<GraphElementSearchResult> searchOnlyForOwnVerticesForAutoCompletionByLabel(String searchTerm, User user) {
        return new Getter<>().get(
                searchTerm,
                true,
                user.username(),
                GraphElementType.Vertex
        );
    }

    @Override
    public List<GraphElementSearchResult> searchOwnTagsForAutoCompletionByLabel(String searchTerm, User user) {
        return new Getter<>().get(
                searchTerm,
                true,
                user.username(),
                GraphElementType.Meta
        );
    }

    @Override
    public List<GraphElementSearchResult> searchRelationsPropertiesSchemasForAutoCompletionByLabel(String searchTerm, User user) {
        return new Getter<GraphElementSearchResult>().get(
                searchTerm,
                false,
                user.username(),
                GraphElementType.Schema,
                GraphElementType.Property,
                GraphElementType.Edge,
                GraphElementType.Meta
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
                GraphElementType.Vertex
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
            try (Session session = driver.session()) {
                StatementResult rs = session.run(
                        String.format(
                                "MATCH (n:GraphElement{uri:$uri}) " +
                                        "WHERE %s " +
                                        "OPTIONAL MATCH (n)-[%s:IDENTIFIED_TO]->(%s) " +
                                        "RETURN %s%s%s labels(n) as type",
                                StringUtils.isEmpty(username) ? "n.shareLevel=40" : "n.owner=$owner",
                                IdentificationQueryBuilder.IDENTIFICATION_RELATION_QUERY_KEY,
                                IdentificationQueryBuilder.IDENTIFIER_QUERY_KEY,
                                FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix("n"),
                                FriendlyResourceQueryBuilder.imageReturnQueryPart("n"),
                                IdentificationQueryBuilder.identificationReturnQueryPart()
                        ),
                        parameters(
                                "uri",
                                uri.toString(),
                                "owner",
                                username
                        )
                );
//            if (!StringUtils.isEmpty(username)) {
//                statement.setString(
//                        "owner",
//                        username
//                );
//            }
                if (!rs.hasNext()) {
                    return null;
                }
                Record record = rs.next();
                return new GraphElementSearchResultPojo(
                        SearchResultGetter.nodeTypeInRow(record),
                        setupGraphElementForDetailedResult(
                                GraphElementFromExtractorQueryRow.usingRowAndKey(
                                        record,
                                        "n"
                                ).build()
                        ),
                        new HashMap<>()
                );
            }
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
            try (Session session = driver.session()) {
                StatementResult rs = session.run(
                        buildQuery(forPersonal, username, graphElementTypes),
                        parameters(
                                "label", formatSearchTerm(searchTerm) + "*",
                                "owner", username
                        )
                );
                return new SearchResultGetter<ResultType>(rs).get();
            }
        }

        private String buildQuery(
                Boolean forPersonal,
                String username,
                GraphElementType... graphElementTypes
        ) {
            String indexDomain = graphElementTypes.length == 4 ? "graphElementLabel" : "vertexLabel";
            return
                    String.format(
                            "CALL db.index.fulltext.queryNodes('%s', $label) YIELD node as n, score " +
                                    "WHERE n." + (forPersonal ? "owner=$owner" : "shareLevel=40 ") +
                                    (!forPersonal && !StringUtils.isEmpty(username) ? "OR n.owner=$owner " : " ") +
                                    "OPTIONAL MATCH (n)-[idr:IDENTIFIED_TO]->(id) " +
                                    "RETURN " +
                                    "score, n.uri, n.label, n.external_uri, COALESCE(n.n.nb_references, 0) as nbReferences, COALESCE(n.number_of_visits, 0) as nbVisits, n.creation_date, n.last_modification_date, " +
                                    "(CASE WHEN n.owner=$owner THEN n.private_context ELSE n.public_context END) as context, " +
                                    IdentificationQueryBuilder.identificationReturnQueryPart() +
                                    "labels(n) as type " +
                                    "ORDER BY nbVisits DESC," +
                                    "score DESC," +
                                    "nbReferences DESC " +
                                    "LIMIT 10",
                            indexDomain
                    );

        }
    }

    public static String formatSearchTerm(String searchTerm) {
        return searchTerm.replaceAll("[^a-zA-Z0-9\\s]", " ");
    }
}
