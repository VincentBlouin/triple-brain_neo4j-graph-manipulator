package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.tag.TagNeo4J;
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

    public TagPojo build() {
        TagPojo tag = new TagPojo(
                getExternalUri(),
                GraphElementFromExtractorQueryRow.usingRowAndKey(
                        record,
                        key
                ).build()
        );
        tag.setNbRefences(getNbReferences());
        tag.getGraphElement().setChildrenIndex(
                VertexFromExtractorQueryRow.getChildrenIndexes(
                        key,
                        record
                )
        );
        tag.getGraphElement().setColors(
                VertexFromExtractorQueryRow.getColors(
                        key,
                        record
                )
        );
        tag.getGraphElement().setFont(
                VertexFromExtractorQueryRow.getFont(
                        key,
                        record
                )
        );
        return tag;
    }

    private URI getExternalUri() {
        String externalUriKey = key + "." + "external_uri";
        return URI.create(record.get(externalUriKey).asString());
    }

    private Integer getNbReferences() {
        return record.get(
                key + "." + TagNeo4J.props.nb_references.name()
        ).asInt();
    }

}
