/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.identification;

import com.google.inject.Guice;
import com.google.inject.Injector;
import guru.bubl.module.model.graph.GraphElementOperator;
import guru.bubl.module.model.graph.IdentificationPojo;
import guru.bubl.module.model.json.IdentificationJson;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jModule;
import guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.AdminOperationsOnDatabase;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementFactory;
import org.junit.Test;
import org.neo4j.rest.graphdb.util.QueryResult;

import javax.inject.Inject;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class ConvertIdentifications6 extends AdminOperationsOnDatabase {


    @Inject
    Neo4jGraphElementFactory neo4jGraphElementFactory;


    @Test
    public void go() {
        Injector injector = Guice.createInjector(
                Neo4jModule.forTestingUsingRest()
        );
        injector.injectMembers(this);
        String query = "START graph_element=node(*) " +
                "WHERE graph_element.identifications is not null " +
                "RETURN graph_element.uri as graph_element_uri, graph_element.identifications as identifications";
        QueryResult<Map<String, Object>> results = queryEngine.query(
                query,
                map()
        );
        Iterator<Map<String, Object>> it = results.iterator();
        while (it.hasNext()) {
            Map<String, Object> result = it.next();
            Map<URI, IdentificationPojo> identifications = IdentificationJson.fromJson(
                    result.get("identifications").toString()
            );
            GraphElementOperator graphElementOperator = neo4jGraphElementFactory.withUri(
                    URI.create(
                            result.get("graph_element_uri").toString()
                    )
            );
            for (IdentificationPojo identification : identifications.values()) {
                switch (identification.getType().name()) {
                    case "same_as":
                        graphElementOperator.addSameAs(
                                identification
                        );
                    case "type":
                        graphElementOperator.addType(
                                identification
                        );
                    default:
                        graphElementOperator.addGenericIdentification(
                                identification
                        );
                }
            }

        }
    }
}
