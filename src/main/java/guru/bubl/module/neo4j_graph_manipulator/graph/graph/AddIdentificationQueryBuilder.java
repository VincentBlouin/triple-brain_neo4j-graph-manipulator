/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.Neo4jIdentification;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.Neo4jImages;

import java.util.Date;

public class AddIdentificationQueryBuilder {

    private IdentifierPojo identification;
    private Neo4jGraphElementOperator graphElement;
    private Boolean isOriginalReference;

    public static AddIdentificationQueryBuilder usingIdentificationForGraphElement(
            IdentifierPojo identification, Neo4jGraphElementOperator graphElement
    ) {
        return new AddIdentificationQueryBuilder(
                identification,
                graphElement
        );
    }

    protected AddIdentificationQueryBuilder(
            IdentifierPojo identification,
            Neo4jGraphElementOperator graphElement
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
                        "f.%s=@images, " +
                        "f.%s=@%s, " +
                        "f.%s=@creationDate, " +
                        "f.%s=timestamp(), " +
                        "f.%s=0 " + //initial nb references
                        "CREATE UNIQUE n-[r :%s]->f " +
                        "SET r.%s=@relationExternalUri, " +
                        "f.nb_references=f.nb_references + 1, " +
                        Neo4jFriendlyResource.LAST_MODIFICATION_QUERY_PART +
                        "RETURN f.uri as uri, " +
                        "f.external_uri as external_uri, " +
                        "f.%s as label, " +
                        "f.%s as comment, " +
                        "f.%s as images, " +
                        "f.%s as creation_date, " +
                        "f.%s as last_modification_date, " +
                        "f.%s as nbReferences",
                this.graphElement.queryPrefix(),
                Neo4jIdentification.props.external_uri,
                Neo4jFriendlyResource.props.owner,
                Neo4jFriendlyResource.props.type,
                GraphElementType.meta,
                Neo4jFriendlyResource.props.label,
                Neo4jFriendlyResource.props.comment,
                Neo4jImages.props.images,
                Neo4jFriendlyResource.props.type,
                Neo4jFriendlyResource.props.type,
                Neo4jFriendlyResource.props.creation_date,
                Neo4jFriendlyResource.props.last_modification_date,
                Neo4jIdentification.props.nb_references,
                Relationships.IDENTIFIED_TO,
                Neo4jIdentification.props.relation_external_uri,
                Neo4jFriendlyResource.props.label,
                Neo4jFriendlyResource.props.comment,
                Neo4jImages.props.images,
                Neo4jFriendlyResource.props.creation_date,
                Neo4jFriendlyResource.props.last_modification_date,
                Neo4jIdentification.props.nb_references
        );
    }
}
