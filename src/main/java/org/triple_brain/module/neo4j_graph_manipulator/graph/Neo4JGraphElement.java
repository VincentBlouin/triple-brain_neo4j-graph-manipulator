package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.hp.hpl.jena.vocabulary.RDFS;
import org.joda.time.DateTime;
import org.neo4j.graphdb.PropertyContainer;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.graph.GraphElement;

import java.net.URI;
import java.util.Date;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JGraphElement implements GraphElement {

    public static final String CREATION_DATE_PROPERTY_NAME = "creation_date";
    public static final String LAST_MODIFICATION_DATE_PROPERTY_NAME = "last_modification_date";

    private PropertyContainer propertyContainer;
    private User owner;

    public static Neo4JGraphElement withPropertyContainerAndOwner(PropertyContainer propertyContainer, User owner){
        return new Neo4JGraphElement(propertyContainer, owner);
    }

    public static Neo4JGraphElement initiatePropertiesAndSetOwner(PropertyContainer propertyContainer, URI uri, User owner){
        propertyContainer.setProperty(
                Neo4JUserGraph.URI_PROPERTY_NAME,
                uri.toString()
        );
        Neo4JGraphElement neo4JGraphElement = new Neo4JGraphElement(
                propertyContainer,
                owner
        );
        neo4JGraphElement.label("");
        Long creationDate = new Date().getTime();
        propertyContainer.setProperty(
                CREATION_DATE_PROPERTY_NAME,
                creationDate
        );
        propertyContainer.setProperty(
                LAST_MODIFICATION_DATE_PROPERTY_NAME,
                creationDate
        );
        return neo4JGraphElement;
    }

    protected Neo4JGraphElement(PropertyContainer propertyContainer, User owner){
        this.propertyContainer = propertyContainer;
        this.owner = owner;
    }

    @Override
    public DateTime creationDate() {
        return new DateTime( (Long) propertyContainer.getProperty(
                CREATION_DATE_PROPERTY_NAME
        ));
    }

    @Override
    public DateTime lastModificationDate() {
        return new DateTime( (Long) propertyContainer.getProperty(
                LAST_MODIFICATION_DATE_PROPERTY_NAME
        ));
    }

    public void updateLastModificationDate(){
        propertyContainer.setProperty(
                LAST_MODIFICATION_DATE_PROPERTY_NAME,
                new Date().getTime()
        );
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
        updateLastModificationDate();
    }

    @Override
    public boolean hasLabel() {
        return propertyContainer.hasProperty(RDFS.label.getURI());
    }

    @Override
    public User owner() {
        return owner;
    }
}
