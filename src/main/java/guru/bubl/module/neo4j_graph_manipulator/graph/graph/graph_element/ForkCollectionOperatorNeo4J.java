package guru.bubl.module.neo4j_graph_manipulator.graph.graph.graph_element;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.graph.graph_element.ForkCollectionOperator;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;

import java.net.URI;
import java.util.Set;

import static org.neo4j.driver.v1.Values.parameters;

public class ForkCollectionOperatorNeo4J implements ForkCollectionOperator {

    @Inject
    private Driver driver;

    private Set<URI> uris;

    @AssistedInject
    protected ForkCollectionOperatorNeo4J(
            @Assisted Set<URI> uris
    ) {
        this.uris = uris;
    }

    @Override
    public void remove() {
        try (Session session = driver.session()) {
            session.run(
                    "MATCH (n:Resource) " +
                            "WHERE n.uri in $uris " +
                            "OPTIONAL MATCH (n)<-[:SOURCE|DESTINATION]-(e:Edge) " +
                            "WITH e, n " +
                            "DETACH DELETE n, e",
                    parameters(
                            "uris",
                            urisToString(uris)
                    )
            );
        }
    }

    private String[] urisToString(Set<URI> uris) {
        return uris.stream().map(URI::toString).toArray(String[]::new);
    }
}
