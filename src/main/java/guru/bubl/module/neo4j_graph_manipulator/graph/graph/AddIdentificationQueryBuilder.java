/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.IdentificationNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.ImagesNeo4j;

public class AddIdentificationQueryBuilder {

    private IdentifierPojo identification;
    private GraphElementOperatorNeo4j graphElement;
    private Boolean isOriginalReference;

    public static AddIdentificationQueryBuilder usingIdentificationForGraphElement(
            IdentifierPojo identification, GraphElementOperatorNeo4j graphElement
    ) {
        return new AddIdentificationQueryBuilder(
                identification,
                graphElement
        );
    }

    protected AddIdentificationQueryBuilder(
            IdentifierPojo identification,
            GraphElementOperatorNeo4j graphElement
    ) {
        this.identification = identification;
        this.graphElement = graphElement;
    }

    public String build() {
        return String.format(
                "%sMERGE (f {%s: @external_uri, %s: @owner}) " +
                        "ON CREATE SET f.uri = @uri, " +
                        "f.%s='%s', " + // graph element type = meta
                        "f.%s=@label, " +
                        "f.%s=@comment, " +
                        "f.private_context=@privateContext, " +
                        "f.public_context=@publicContext, " +
                        "f.%s=@images, " +
                        "f.%s=@%s, " +
                        "f.%s=@creationDate, " +
                        "f.%s=timestamp(), " +
                        "f.%s=0 " + //initial nb references
                        "CREATE UNIQUE n-[r :%s]->f " +
                        "SET r.%s=@relationExternalUri, " +
                        "f.nb_references=f.nb_references + 1, " +
                        FriendlyResourceNeo4j.LAST_MODIFICATION_QUERY_PART +
                        "RETURN f.uri as uri, " +
                        "f.external_uri as external_uri, " +
                        "f.%s as label, " +
                        "f.%s as comment, " +
                        "f.%s as images, " +
                        "f.%s as creation_date, " +
                        "f.%s as last_modification_date, " +
                        "f.%s as nbReferences",
                this.graphElement.queryPrefix(),
                IdentificationNeo4j.props.external_uri,
                FriendlyResourceNeo4j.props.owner,
                FriendlyResourceNeo4j.props.type,
                GraphElementType.meta,
                FriendlyResourceNeo4j.props.label,
                FriendlyResourceNeo4j.props.comment,
                ImagesNeo4j.props.images,
                FriendlyResourceNeo4j.props.type,
                FriendlyResourceNeo4j.props.type,
                FriendlyResourceNeo4j.props.creation_date,
                FriendlyResourceNeo4j.props.last_modification_date,
                IdentificationNeo4j.props.nb_references,
                Relationships.IDENTIFIED_TO,
                IdentificationNeo4j.props.relation_external_uri,
                FriendlyResourceNeo4j.props.label,
                FriendlyResourceNeo4j.props.comment,
                ImagesNeo4j.props.images,
                FriendlyResourceNeo4j.props.creation_date,
                FriendlyResourceNeo4j.props.last_modification_date,
                IdentificationNeo4j.props.nb_references
        );
    }
}
