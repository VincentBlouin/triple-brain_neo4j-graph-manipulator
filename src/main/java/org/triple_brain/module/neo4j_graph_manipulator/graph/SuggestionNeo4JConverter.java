package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.Suggestion;
import org.triple_brain.module.model.TripleBrainUris;

import javax.inject.Inject;
import java.util.UUID;

/*
* Copyright Mozilla Public License 1.1
*/
public class SuggestionNeo4JConverter {

   @Inject
   private GraphDatabaseService graphDb;

    public Node suggestionToNode(Suggestion suggestion){
        Node node = graphDb.createNode();
        node.setProperty(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                TripleBrainUris.BASE + "suggestion/" + UUID.randomUUID().toString()
                );
        addType(node, suggestion);
        addDomain(node, suggestion);
        addLabel(node, suggestion);
        return node;
    }

    private void addType(Node node, Suggestion suggestion){
        node.setProperty(
                RDF.type.getURI(),
                suggestion.typeUri().toString()
        );
    }
    private void addDomain(Node node, Suggestion suggestion){
        node.setProperty(
                RDFS.domain.getURI(),
                suggestion.domainUri().toString()
        );
    }
    private void addLabel(Node node, Suggestion suggestion){
        node.setProperty(
                RDFS.label.getURI(),
                suggestion.label().toString()
        );
    }

    public Suggestion nodeToSuggestion(Node node){
        return Suggestion.withTypeDomainAndLabel(
                Uris.get(node.getProperty(RDF.type.getURI()).toString()),
                Uris.get(node.getProperty(RDFS.domain.getURI()).toString()),
                node.getProperty(RDFS.label.getURI()).toString()
        );
    }
}
