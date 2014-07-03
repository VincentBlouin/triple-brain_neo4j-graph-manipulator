package org.triple_brain.module.neo4j_graph_manipulator.graph.embedded.admin;

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

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
            Set<SuggestionPojo> suggestions = new HashSet<>();
            Map<URI, SuggestionPojo> suggestionsMap = (Map<URI, SuggestionPojo>) vertex.getSuggestions();
            suggestions.addAll(suggestionsMap.values());
            vertex.setSuggestions(suggestions);
        }
    }
}
