/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element;

import com.google.gson.reflect.TypeToken;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.center_graph_element.CenterGraphElementPojo;
import guru.bubl.module.model.center_graph_element.CenteredGraphElementsOperator;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.json.JsonUtils;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;
import java.util.*;

import static org.neo4j.driver.v1.Values.parameters;

public class CenterGraphElementsOperatorNeo4j implements CenteredGraphElementsOperator {

    private Session session;
    private User user;

    @AssistedInject
    protected CenterGraphElementsOperatorNeo4j(
            Session session,
            @Assisted User user
    ) {
        this.session = session;
        this.user = user;
    }

    @Override
    public Set<CenterGraphElementPojo> getPublicAndPrivate() {
        return getPublicOnlyOrNot(
                false,
                null,
                null
        );
    }

    @Override
    public Set<CenterGraphElementPojo> getPublicAndPrivateWithLimitAndSkip(Integer limit, Integer skip) {
        return getPublicOnlyOrNot(
                false,
                limit,
                skip
        );
    }

    @Override
    public Set<CenterGraphElementPojo> getPublicOnlyOfType() {
        return getPublicOnlyOrNot(
                true,
                null,
                null
        );
    }

    @Override
    public Set<CenterGraphElementPojo> getPublicOnlyOfTypeWithLimitAndSkip(Integer limit, Integer skip) {
        return getPublicOnlyOrNot(
                true,
                limit,
                skip
        );
    }

    private Set<CenterGraphElementPojo> getPublicOnlyOrNot(Boolean publicOnly, Integer limit, Integer skip) {
        Set<CenterGraphElementPojo> centerGraphElements = new HashSet<>();
        StatementResult rs = session.run(
                String.format(
                        "MATCH(n:GraphElement) " +
                                "WHERE n.owner=$owner AND EXISTS(n.last_center_date) " +
                                (publicOnly ? "AND n.shareLevel=40 " : "") +
                                "RETURN n.%s as context, n.number_of_visits as numberOfVisits, n.last_center_date as lastCenterDate, n.label as label, n.uri as uri, n.nb_references as nbReferences, n.colors as colors, n.shareLevel " +
                                "%s " +
                                "%s " +
                                "%s",
                        publicOnly ? "public_context" : "private_context",
                        limit == null ? "" : "ORDER BY n.last_center_date DESC",
                        skip == null ? "" : " SKIP " + skip,
                        limit == null ? "" : " LIMIT " + limit
                ),
                parameters(
                        "owner", user.username()
                )
        );
        while (rs.hasNext()) {
            Record record = rs.next();
            Date lastCenterDate = null == record.get("lastCenterDate").asObject() ?
                    null :
                    new Date(record.get("lastCenterDate").asLong());
            Integer numberOfVisits = null == record.get("numberOfVisits").asObject() ?
                    null :
                    record.get("numberOfVisits").asInt();
            Integer nbReferences = null == record.get("nbReferences").asObject() ?
                    null :
                    record.get("nbReferences").asInt();
            String colors = record.get("colors").asString();
            ShareLevel shareLevel = record.get("n.shareLevel").asObject() == null ? ShareLevel.PRIVATE : ShareLevel.get(
                    record.get("n.shareLevel").asInt()
            );
            GraphElementPojo graphElement = new GraphElementPojo(new FriendlyResourcePojo(
                    URI.create(record.get("uri").asString()),
                    record.get("label").asString()
            ));
            graphElement.setColors(colors);
            centerGraphElements.add(
                    new CenterGraphElementPojo(
                            numberOfVisits,
                            lastCenterDate,
                            graphElement,
                            getContextFromRow(record),
                            nbReferences,
                            shareLevel
                    )
            );
        }
        return centerGraphElements;
    }

    public static Map<URI, String> getContextFromRow(Record record) {
        String contextStr = record.get("context").asString();
        if (null == contextStr) {
            return new HashMap<>();
        }
        return JsonUtils.getGson().fromJson(
                contextStr,
                new TypeToken<Map<URI, String>>() {
                }.getType()
        );
    }
}
