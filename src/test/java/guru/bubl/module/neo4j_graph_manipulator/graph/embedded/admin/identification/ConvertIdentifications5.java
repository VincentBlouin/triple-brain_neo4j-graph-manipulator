/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.identification;

import guru.bubl.module.model.graph.*;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.rest.graphdb.util.QueryResult;
import guru.bubl.module.model.json.IdentificationJson;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.AdminOperationsOnDatabase;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementOperator;

import javax.inject.Inject;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;
@Ignore
public class ConvertIdentifications5 extends AdminOperationsOnDatabase {

    @Inject
    IdentificationFactory identificationFactory;

    @Inject
    Neo4jGraphElementFactory neo4jGraphElementFactory;

    @Test
    public void go() {
        String query = "START  graph_element=node(*) " +
                "MATCH graph_element-[identification_relation:" +
                Relationships.IDENTIFIED_TO + "|" +
                Relationships.TYPE + "|" +
                Relationships.SAME_AS + "]->identification " +
                "RETURN graph_element.uri as graph_element_uri, identification.uri as identification_uri, " +
                "type(identification_relation) as in_path_node_identification_type";
//        System.out.println(query);
        QueryResult<Map<String, Object>> results = queryEngine.query(
                query,
                map()
        );
        Map<Neo4jGraphElementOperator, Map<URI, IdentificationPojo>> identifications = new HashMap<>();
        Iterator<Map<String, Object>> it = results.iterator();
        while (it.hasNext()) {
            Map<String, Object> result = it.next();
            if(!result.containsKey("identification_uri")){
                System.out.println("wtf");
            }
            IdentificationOperator neo4jIdentification = identificationFactory.withUri(URI.create(
                            result.get("identification_uri").toString()
                    )
            );
            FriendlyResourcePojo friendlyResourcePojo = new FriendlyResourcePojo(
                    neo4jIdentification
            );
            IdentificationPojo identificationPojo = new IdentificationPojo(
                    friendlyResourcePojo
            );
            identificationPojo.setExternalResourceUri(
                    neo4jIdentification.getExternalResourceUri()
            );
            IdentificationType identificationType;
            String identificationTypeStr = result.get("in_path_node_identification_type").toString();
            if (identificationTypeStr.equals(Relationships.SAME_AS.toString())) {
                identificationType = IdentificationType.same_as;
            } else if (
                    identificationTypeStr.equals(Relationships.TYPE.toString())
                    ) {
                identificationType = IdentificationType.type;
            } else if (
                    identificationTypeStr.equals(Relationships.IDENTIFIED_TO.toString())
                    ) {
                identificationType = IdentificationType.generic;
            } else {
                throw new RuntimeException("unknown relation type");
            }
            identificationPojo.setType(
                    identificationType
            );
            Neo4jGraphElementOperator graphElementOperator = neo4jGraphElementFactory.withUri(
                    URI.create(
                            result.get("graph_element_uri").toString()
                    )
            );
            if (identifications.containsKey(graphElementOperator)) {
                identifications.get(graphElementOperator).put(
                        identificationPojo.getExternalResourceUri(),
                        identificationPojo
                );
            } else {
                Map<URI, IdentificationPojo> identificationsPojo = new HashMap<>();
                identificationsPojo.put(
                        identificationPojo.getExternalResourceUri(),
                        identificationPojo
                );
                identifications.put(graphElementOperator, identificationsPojo);
            }
        }
        for (Map.Entry<Neo4jGraphElementOperator, Map<URI, IdentificationPojo>> entry : identifications.entrySet()) {
            setIdentifications(
                    entry.getKey(),
                    entry.getValue()
            );
        }
    }

    private void setIdentifications(Neo4jGraphElementOperator graphElementOperator, Map<URI, IdentificationPojo> identifications) {
        queryEngine.query(
                graphElementOperator.queryPrefix() +
                        "SET n." + Neo4jGraphElementOperator.props.identifications + "= { " + Neo4jGraphElementOperator.props.identifications + "} ",
                map(
                        Neo4jGraphElementOperator.props.identifications.name(), IdentificationJson.toJson(identifications)
                )
        );
    }
}

