/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.embedded;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jModule;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;

import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

@Ignore("for manual test only")
public class QueryEngineUsingEmbeddedTest {

    GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(
            Neo4jModule.DB_PATH_FOR_TESTS
    )
            .setConfig(
                    GraphDatabaseSettings.node_keys_indexable,
                    Neo4jUserGraph.URI_PROPERTY_NAME
            ).setConfig(
                    GraphDatabaseSettings.node_auto_indexing,
                    "true"
            ).setConfig(
                    GraphDatabaseSettings.relationship_keys_indexable,
                    Neo4jUserGraph.URI_PROPERTY_NAME
            ).setConfig(
                    GraphDatabaseSettings.relationship_auto_indexing,
                    "true"
            ).newGraphDatabase();

    QueryEngine<Map<String, Object>> queryEngine = new QueryEngineUsingEmbedded(
            graphDb
    );

    @Test
    public void can_query() {
        queryEngine.query(
                "CREATE (n {props})",
                map(
                        "props",
                        map(
                                "pomme", "avion"
                        )
                )
        );
        QueryResult<Map<String, Object>> result = queryEngine.query(
                "START n=node(*) WHERE n.pomme='avion' RETURN n.pomme as pomme",
                map()
        );
        assertThat(
                result.iterator().next().get("pomme").toString(),
                is(
                        "avion"
                )
        );
    }
}
