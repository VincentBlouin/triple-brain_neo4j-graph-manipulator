package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;
import org.triple_brain.module.model.suggestion.SuggestionOriginPojo;
import org.triple_brain.module.model.suggestion.SuggestionPojo;
import java.util.HashSet;
import java.util.Map;

public class SuggestionExtractorQueryRow {

    private Map<String, Object> row;

    private String keyPrefix;

    public SuggestionExtractorQueryRow(
            Map<String, Object> row,
            String keyPrefix
    ) {
        this.row = row;
        this.keyPrefix = keyPrefix;
    }

    public SuggestionPojo build() {
        SuggestionPojo suggestion = init();
        update(suggestion);
        return suggestion;
    }

    private SuggestionPojo init() {
        SuggestionPojo suggestion = new SuggestionPojo(
                FriendlyResourceFromExtractorQueryRow.usingRowAndPrefix(
                        row,
                        keyPrefix + "_suggestion"
                ).build(),
                FriendlyResourceFromExtractorQueryRow.usingRowAndPrefix(
                        row,
                        keyPrefix + "_suggestion_same_as"
                ).build(),
                FriendlyResourceFromExtractorQueryRow.usingRowAndPrefix(
                        row,
                        keyPrefix + "_suggestion_domain"
                ).build(),
                new HashSet<SuggestionOriginPojo>()
        );
        suggestion.addOrigin(
                suggestionOriginFromRow()
        );
        return suggestion;
    }

    public SuggestionPojo update(SuggestionPojo suggestion){
        suggestion.addOrigin(
                suggestionOriginFromRow()
        );
        return suggestion;
    }

    private SuggestionOriginPojo suggestionOriginFromRow() {
        return new SuggestionOriginPojo(
                FriendlyResourceFromExtractorQueryRow.usingRowAndPrefix(
                        row,
                        keyPrefix + "_suggestion_origin"
                ).build(),
                row.get(
                        keyPrefix + "_suggestion_origin.origin"
                ).toString()
        );
    }
}
