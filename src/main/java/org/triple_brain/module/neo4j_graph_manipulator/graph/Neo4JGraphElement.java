package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.hp.hpl.jena.vocabulary.RDFS;
import org.neo4j.graphdb.PropertyContainer;
import org.triple_brain.module.model.graph.GraphElement;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JGraphElement implements GraphElement {

    private PropertyContainer propertyContainer;

    public static Neo4JGraphElement withPropertyContainer(PropertyContainer propertyContainer){
        return new Neo4JGraphElement(propertyContainer);
    }

    public static Neo4JGraphElement initiateProperties(PropertyContainer propertyContainer, URI uri){
        propertyContainer.setProperty(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        );
        Neo4JGraphElement neo4JGraphElement = new Neo4JGraphElement(
                propertyContainer
        );
        neo4JGraphElement.label("");
        return neo4JGraphElement;
    }

    protected Neo4JGraphElement(PropertyContainer propertyContainer){
        this.propertyContainer = propertyContainer;
    }

    @Override
    public String id() {
        return propertyContainer.getProperty(Neo4JUserGraph.URI_PROPERTY_NAME).toString();
    }

    @Override
    public String label() {
        return propertyContainer.getProperty(RDFS.label.getURI()).toString();
    }

    @Override
    public void label(String label) {
        propertyContainer.setProperty(RDFS.label.getURI(), label);
    }

    @Override
    public boolean hasLabel() {
        return propertyContainer.hasProperty(RDFS.label.getURI());
    }
}
