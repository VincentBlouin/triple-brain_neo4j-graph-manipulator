/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.suggestion;


import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jModule;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.rest.graphdb.util.QueryResult;
import guru.bubl.module.model.WholeGraph;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin.AdminOperationsOnDatabase;

import java.util.Iterator;
import java.util.Map;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

@Ignore
public class ConvertOldSuggestionsToNew extends AdminOperationsOnDatabase {

//    @Inject
//    WholeGraph wholeGraph;
//
//    private static Gson gson = new Gson();
//
//    @Test
//    public void go(){
//        Injector injector = Guice.createInjector(
//                Neo4jModule.forTestingUsingRest()
//        );
//        injector.injectMembers(this);
//        Iterator<VertexInSubGraphOperator> vertexIt = wholeGraph.getAllVertices();
//        while(vertexIt.hasNext()){
//            Neo4jVertexInSubGraphOperator vertex = (Neo4jVertexInSubGraphOperator) vertexIt.next();
//            QueryResult<Map<String, Object>> result = queryEngine.query(
//                    vertex.queryPrefix() +
//                            "return n.`" + Neo4jVertexInSubGraphOperator.props.suggestions + "` as suggestions",
//                    map()
//            );
//            Object suggestionsValue = result.iterator().next().get("suggestions");
//            if(suggestionsValue != null){
//                convertSuggestion(suggestionsValue, vertex);
//            }
//        }
//    }

    private void convertSuggestion(Object suggestionsValue, Neo4jVertexInSubGraphOperator vertex){
//        Set<SuggestionPojoLegacy> suggestionsLegacy = gson.fromJson(
//                suggestionsValue.toString(),
//                new TypeToken<Set<SuggestionPojoLegacy>>() {
//                }.getType()
//        );
//        Set<SuggestionPojo> suggestionsConverted = new HashSet<>();
//        for(SuggestionPojoLegacy suggestionPojoLegacy : suggestionsLegacy){
//            FriendlyResourcePojo friendlyResourcePojo = new FriendlyResourcePojo(
//                    suggestionPojoLegacy.uri(),
//                    suggestionPojoLegacy.label()
//            );
//            SuggestionPojo converted = new SuggestionPojo(
//                    friendlyResourcePojo,
//                    suggestionPojoLegacy.sameAs().uri(),
//                    suggestionPojoLegacy.domain().uri(),
//                    suggestionPojoLegacy.origins()
//            );
//            suggestionsConverted.add(converted);
//        }
//        vertex.setSuggestions(suggestionsConverted);
    }
}
