package guru.bubl.module.neo4j_graph_manipulator.graph.graph.pattern;

import com.google.inject.Inject;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.pattern.PatternList;
import guru.bubl.module.model.graph.pattern.PatternPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element.CenterGraphElementsOperatorNeo4j;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class PatternListNeo4J implements PatternList {

    @Inject
    private Session session;

    @Override
    public Set<PatternPojo> get() {
        Set<PatternPojo> patterns = new HashSet<>();
        StatementResult rs = session.run(
                "MATCH(n:Pattern) RETURN n.private_context as context, n.creation_date as creationDate, n.label as label, n.uri as uri"
        );
        while (rs.hasNext()) {
            Record record = rs.next();
            Long creationDate = null == record.get("creationDate").asObject() ?
                    null :
                    record.get("creationDate").asLong();
            GraphElementPojo graphElement = new GraphElementPojo(new FriendlyResourcePojo(
                    URI.create(record.get("uri").asString()),
                    record.get("label").asString()
            ));
            graphElement.setCreationDate(creationDate);
            patterns.add(
                    new PatternPojo(
                            graphElement,
                            CenterGraphElementsOperatorNeo4j.getContextFromRow(record)
                    )
            );
        }
        return patterns;
    }
}
