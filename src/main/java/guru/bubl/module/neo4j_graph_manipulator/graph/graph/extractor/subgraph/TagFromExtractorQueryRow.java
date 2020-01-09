package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceFromExtractorQueryRow;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.IdentificationNeo4j;
import org.neo4j.driver.v1.Record;

import java.net.URI;

public class TagFromExtractorQueryRow {

    private Record record;
    private String key;

    public static TagFromExtractorQueryRow usingRowAndKey(
            Record record,
            String key
    ) {
        return new TagFromExtractorQueryRow(
                record,
                key
        );
    }

    protected TagFromExtractorQueryRow(Record record, String key) {
        this.record = record;
        this.key = key;
    }

    public IdentifierPojo build() {
        IdentifierPojo tag = new IdentifierPojo(
                getExternalUri(),
                FriendlyResourceFromExtractorQueryRow.usingRowAndNodeKey(
                        record,
                        key
                ).build()
        );
        tag.setNbRefences(getNbReferences());
        return tag;
    }

    private URI getExternalUri() {
        String externalUriKey = key + "." + "external_uri";
        return URI.create(record.get(externalUriKey).asString());
    }

    private Integer getNbReferences() {
        return record.get(
                key + "." + IdentificationNeo4j.props.nb_references.name()
        ).asInt();
    }

}
