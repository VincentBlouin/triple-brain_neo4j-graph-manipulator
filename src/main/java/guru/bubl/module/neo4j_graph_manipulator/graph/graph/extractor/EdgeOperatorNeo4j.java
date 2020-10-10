package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.graph.graph_element.GraphElement;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementSpecialOperatorFactory;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

import java.net.URI;
import java.util.Date;

import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementOperatorNeo4j.decrementNbNeighborsQueryPart;
import static guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element.GraphElementOperatorNeo4j.incrementNbNeighborsQueryPart;
import static org.neo4j.driver.Values.parameters;

public class EdgeOperatorNeo4j implements EdgeOperator, OperatorNeo4j {

    private URI uri;

    @Inject
    private GraphElementSpecialOperatorFactory graphElementSpecialOperatorFactory;

    @Inject
    private Driver driver;

    @AssistedInject
    protected EdgeOperatorNeo4j(
            @Assisted URI uri
    ) {
        this.uri = uri;
    }

    @Override
    public void changeSource(
            URI newSourceUri,
            ShareLevel oldEndShareLevel,
            ShareLevel keptEndShareLevel,
            ShareLevel newEndShareLevel
    ) {
        changeEndVertex(
                newSourceUri,
                Relationships.SOURCE,
                oldEndShareLevel,
                keptEndShareLevel,
                newEndShareLevel
        );
    }

    @Override
    public void changeDestination(
            URI newDestinationUri,
            ShareLevel oldEndShareLevel,
            ShareLevel keptEndShareLevel,
            ShareLevel newEndShareLevel
    ) {
        changeEndVertex(
                newDestinationUri,
                Relationships.DESTINATION,
                oldEndShareLevel,
                keptEndShareLevel,
                newEndShareLevel
        );
    }

    private void changeEndVertex(
            URI newEndUri,
            Relationships relationshipToChange,
            ShareLevel oldEndShareLevel,
            ShareLevel keptEndShareLevel,
            ShareLevel newEndShareLevel
    ) {
        Relationships relationshipToKeep = Relationships.SOURCE == relationshipToChange ?
                Relationships.DESTINATION : Relationships.SOURCE;
        String decrementPreviousVertexQueryPart = decrementNbNeighborsQueryPart(
                keptEndShareLevel,
                "prev_v",
                "SET "
        );
        String incrementKeptVertexQueryPart = "";
        String decrementKeptVertexQueryPart = "";
        if (oldEndShareLevel.isSame(keptEndShareLevel)) {
            if (!newEndShareLevel.isSame(keptEndShareLevel)) {
                incrementKeptVertexQueryPart = incrementNbNeighborsQueryPart(
                        newEndShareLevel,
                        "kept_v",
                        ", "
                );
            }
        } else {
            decrementKeptVertexQueryPart = decrementNbNeighborsQueryPart(
                    oldEndShareLevel,
                    "kept_v",
                    ", "
            );
        }

        String incrementNewEndVertexQueryPart = incrementNbNeighborsQueryPart(
                keptEndShareLevel,
                "new_v",
                ", "
        );
        String query = String.format(
                "%s, (new_v:Resource{uri:$endVertexUri}), " +
                        "(n)-[prev_rel:%s]->(prev_v) " +
                        "OPTIONAL MATCH (n)-[:%s]->(kept_v) " +
                        "MERGE (n)-[:%s]->(new_v) " +
                        "DELETE prev_rel " +
                        decrementPreviousVertexQueryPart +
                        decrementKeptVertexQueryPart +
                        incrementKeptVertexQueryPart +
                        incrementNewEndVertexQueryPart + ",%s",
                queryPrefix(),
                relationshipToChange,
                relationshipToKeep,
                relationshipToChange,
                FriendlyResourceNeo4j.LAST_MODIFICATION_QUERY_PART
        );
        try (Session session = driver.session()) {
            session.run(
                    query,
                    parameters(
                            "uri",
                            this.uri().toString(),
                            "endVertexUri",
                            newEndUri.toString(),
                            "last_modification_date",
                            new Date().getTime()
                    )
            );
        }
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public URI sourceUri() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() +
                            "MATCH (n)-[:SOURCE]->(v) " +
                            "RETURN v.uri as uri",
                    parameters(
                            "uri",
                            uri().toString()
                    )
            ).single();
            return URI.create(record.get("uri").asString());
        }
    }

    @Override
    public URI destinationUri() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() +
                            "MATCH (n)-[:DESTINATION]->(v) " +
                            "RETURN v.uri as uri",
                    parameters(
                            "uri",
                            uri().toString()
                    )
            ).single();
            return URI.create(record.get("uri").asString());
        }
    }

    @Override
    public GraphElement source() {
        return graphElementSpecialOperatorFactory.getFromUri(sourceUri());
    }

    @Override
    public GraphElement destination() {
        return graphElementSpecialOperatorFactory.getFromUri(destinationUri());
    }
}
