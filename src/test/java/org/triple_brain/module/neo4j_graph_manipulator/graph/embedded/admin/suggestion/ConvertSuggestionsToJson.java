/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin.suggestion;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.Ignore;
import org.junit.Test;
import org.triple_brain.module.model.WholeGraph;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphOperator;
import org.triple_brain.module.model.graph.vertex.VertexOperator;
import org.triple_brain.module.model.suggestion.SuggestionPojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jModule;
import org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin.AdminOperationsOnDatabase;

import java.net.URI;
import java.util.*;

@Ignore
public class ConvertSuggestionsToJson extends AdminOperationsOnDatabase {

    @Inject
    WholeGraph wholeGraph;

    @Test
    public void go(){
        Injector injector = Guice.createInjector(
                Neo4jModule.forTestingUsingRest()
        );
        injector.injectMembers(this);
        Iterator<VertexInSubGraphOperator> vertexIt = wholeGraph.getAllVertices();
        while(vertexIt.hasNext()){
            VertexOperator vertex = vertexIt.next();
            Map<URI, SuggestionPojo> suggestions = new HashMap<>();
            Map<URI, SuggestionPojo> suggestionsMap = vertex.getSuggestions();
            suggestions.putAll(suggestionsMap);
            vertex.setSuggestions(suggestions);
        }
    }
}
