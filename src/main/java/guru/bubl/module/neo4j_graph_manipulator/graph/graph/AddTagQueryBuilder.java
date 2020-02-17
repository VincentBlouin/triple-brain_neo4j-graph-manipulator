/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.model.graph.tag.TagPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;

public class AddTagQueryBuilder {

    private TagPojo identification;
    private GraphElementOperatorNeo4j graphElement;

    public static AddTagQueryBuilder usingIdentificationForGraphElement(
            TagPojo identification, GraphElementOperatorNeo4j graphElement
    ) {
        return new AddTagQueryBuilder(
                identification,
                graphElement
        );
    }

    protected AddTagQueryBuilder(
            TagPojo identification,
            GraphElementOperatorNeo4j graphElement
    ) {
        this.identification = identification;
        this.graphElement = graphElement;
    }

    public String build() {
        return String.format(
                "%sMERGE (f:Resource:GraphElement:Meta{external_uri:$external_uri, owner:$owner}) " +
                        "ON CREATE SET f.uri=$metaUri, " +
                        "f.shareLevel=10, " +
                        "f.label=$label, " +
                        "f.comment=$comment, " +
                        "f.private_context=$privateContext, " +
                        "f.public_context=$publicContext, " +
                        "f.images=$images, " +
                        "f.creation_date=$creationDate, " +
                        "f.last_modification_date=timestamp(), " +
                        "f.nb_references=0 " + //initial nb references
                        "CREATE UNIQUE (n)-[r:IDENTIFIED_TO]->(f) " +
                        "SET r.relation_external_uri=$relationExternalUri, " +
                        "f.nb_references=f.nb_references + 1, " +
                        FriendlyResourceNeo4j.LAST_MODIFICATION_QUERY_PART +
                        "RETURN f.uri as uri, " +
                        "f.external_uri as external_uri, " +
                        "f.label as label, " +
                        "f.comment as comment, " +
                        "f.images as images, " +
                        "f.creation_date as creation_date, " +
                        "f.last_modification_date as last_modification_date, " +
                        "f.nb_references as nbReferences, " +
                        "f.shareLevel ",
                this.graphElement.queryPrefix()
        );
    }
}
