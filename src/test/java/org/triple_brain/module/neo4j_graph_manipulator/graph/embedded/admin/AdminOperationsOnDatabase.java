/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin;

import org.junit.BeforeClass;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestAPIFacade;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;

public class AdminOperationsOnDatabase {

    @BeforeClass
    public static void before(){
        System.setProperty(
                "org.neo4j.rest.logging_filter",
                "true"
        );
    }
    protected RestAPI restApi = new RestAPIFacade(
            "http://localhost:9594/db/data"
    );
    protected QueryEngine queryEngine = new RestCypherQueryEngine(restApi);
}
