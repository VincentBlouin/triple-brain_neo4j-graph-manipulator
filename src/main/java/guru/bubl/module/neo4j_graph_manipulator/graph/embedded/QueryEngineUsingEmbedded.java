/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.embedded;

import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.rest.graphdb.util.QueryResult;

import java.util.Map;

public class QueryEngineUsingEmbedded implements org.neo4j.rest.graphdb.query.QueryEngine<java.util.Map<java.lang.String,java.lang.Object>>{

    private ExecutionEngine engine;

    public static QueryEngineUsingEmbedded usingGraphDb(GraphDatabaseService graphDb) {
        return new QueryEngineUsingEmbedded(
                graphDb
        );
    }

    protected QueryEngineUsingEmbedded(GraphDatabaseService graphDb) {
        engine = new ExecutionEngine(
                graphDb,
                StringLogger.SYSTEM
        );
    }

    @Override
    public QueryResult<Map<String, Object>> query(String s, Map<String, Object> stringObjectMap) {
        ExecutionResult result = engine.execute(
                s,
                stringObjectMap
        );
        return new QueryResultUsingEmbedded(
                result
        );
    }
}
