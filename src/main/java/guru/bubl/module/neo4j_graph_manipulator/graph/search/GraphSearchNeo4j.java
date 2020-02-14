/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphSearch;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.TagQueryBuilder;
import org.apache.commons.lang.StringUtils;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.util.List;

import static org.neo4j.driver.v1.Values.parameters;

public class GraphSearchNeo4j implements GraphSearch {


    private Driver driver;
    private Integer limit;
    private Integer skip;
    private String searchTerm;

    @AssistedInject
    protected GraphSearchNeo4j(
            Driver driver,
            @Assisted String searchTerm
    ) {
        this(driver, searchTerm, 0, GraphSearch.LIMIT);
    }

    @AssistedInject
    protected GraphSearchNeo4j(
            Driver driver,
            @Assisted String searchTerm,
            @Assisted("skip") Integer skip,
            @Assisted("limit") Integer limit
    ) {
        this.driver = driver;
        this.searchTerm = searchTerm;
        this.skip = skip;
        this.limit = limit;
    }


    @Override
    public List<GraphElementSearchResult> searchForAllOwnResources(User user) {
        return new Getter<>().get(
                true,
                user.username(),
                "graphElementLabel"
        );
    }

    @Override
    public List<GraphElementSearchResult> searchForAnyResourceThatCanBeUsedAsAnIdentifier(User user) {
        return new Getter<>().get(
                false,
                user.username(),
                "graphElementLabel"
        );
    }

    @Override
    public List<GraphElementSearchResult> searchOnlyForOwnVerticesForAutoCompletionByLabel(User user) {
        return new Getter<>().get(
                true,
                user.username(),
                "vertexLabel"
        );
    }

    @Override
    public List<GraphElementSearchResult> searchOwnTagsForAutoCompletionByLabel(User user) {
        return new Getter<>().get(
                true,
                user.username(),
                "tagLabel"
        );
    }

    @Override
    public List<GraphElementSearchResult> searchRelationsForAutoCompletionByLabel(User user) {
        return new Getter<GraphElementSearchResult>().get(
                false,
                user.username(),
                "graphElementLabel"
        );
    }

    @Override
    public List<GraphElementSearchResult> searchPublicVerticesOnly() {
        return new Getter<GraphElementSearchResult>().get(
                false,
                "",
                "vertexLabel"
        );
    }

    private class Getter<ResultType extends GraphElementSearchResult> {

        public List<ResultType> get(
                Boolean forPersonal,
                String username,
                String indexDomain
        ) {
            try (Session session = driver.session()) {
                StatementResult rs = session.run(
                        buildQuery(forPersonal, username, indexDomain),
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
                String indexDomain
        ) {
            return
                    String.format(
                            "CALL db.index.fulltext.queryNodes('%s', $label) YIELD node as n, score " +
                                    "WHERE n." + (forPersonal ? "owner=$owner" : "shareLevel=40 ") +
                                    (!forPersonal && !StringUtils.isEmpty(username) ? "OR n.owner=$owner " : " ") +
                                    "OPTIONAL MATCH (n)-[idr:IDENTIFIED_TO]->(id) " +
                                    "WHERE id." + (forPersonal ? "owner=$owner" : " shareLevel=40 ") +
                                    (!forPersonal && !StringUtils.isEmpty(username) ? "OR id.owner=$owner " : " ") +
                                    "RETURN " +
                                    "score, n.uri, n.label, n.external_uri, COALESCE(n.nb_references, 0) as nbReferences, COALESCE(n.number_of_visits, 0) as nbVisits, n.creation_date, n.last_modification_date, " +
                                    "(CASE WHEN n.owner=$owner THEN n.private_context ELSE n.public_context END) as context, " +
                                    TagQueryBuilder.identificationReturnQueryPart() +
                                    "labels(n) as type " +
                                    "ORDER BY nbVisits DESC," +
                                    "score DESC," +
                                    "nbReferences DESC " +
                                    "SKIP " + skip +
                                    " LIMIT " + limit,
                            indexDomain
                    );

        }
    }

    public static String formatSearchTerm(String searchTerm) {
        return searchTerm.replaceAll("[?!*|~|(|)\\s]", " ");
    }
}
