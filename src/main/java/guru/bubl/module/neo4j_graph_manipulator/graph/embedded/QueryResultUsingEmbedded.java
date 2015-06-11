/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.embedded;

import org.neo4j.cypher.ExecutionResult;
import org.neo4j.rest.graphdb.util.ConvertedResult;
import org.neo4j.rest.graphdb.util.Handler;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.neo4j.rest.graphdb.util.ResultConverter;
import java.util.Iterator;
import java.util.Map;

public class QueryResultUsingEmbedded implements QueryResult<java.util.Map<String, Object>>{

    private ExecutionResult result;

    public QueryResultUsingEmbedded(ExecutionResult result){
        this.result = result;
    }

    @Override
    public <R> ConvertedResult<R> to(Class<R> rClass) {
        return null;
    }

    @Override
    public <R> ConvertedResult<R> to(Class<R> rClass, ResultConverter<Map<String, Object>, R> trResultConverter) {
        return null;
    }

    @Override
    public void handle(Handler<Map<String, Object>> tHandler) {}

    @Override
    public Iterator<Map<String, Object>> iterator() {
        return result.javaIterator();
    }
}
