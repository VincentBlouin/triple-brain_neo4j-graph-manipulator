package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.graph.FriendlyResourceOperator;
import org.triple_brain.module.model.json.FriendlyResourceJson;

import javax.inject.Inject;
import java.net.URI;
import java.util.Date;
import java.util.Set;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jFriendlyResource implements FriendlyResourceOperator{

    public static final String CREATION_DATE_PROPERTY_NAME = "creation_date";
    public static final String LAST_MODIFICATION_DATE_PROPERTY_NAME = "last_modification_date";

    @Inject
    Neo4jImageUtils imageUtils;

    @Inject
    Neo4jUtils neo4jUtils;

    protected Node node;

    @AssistedInject
    protected Neo4jFriendlyResource(
            @Assisted Node node
    ) {
        this.node = node;
        if(!hasCreationDate()){
            initCreationAndLastModificationDate();
        }
    }

    @AssistedInject
    protected Neo4jFriendlyResource(
            Neo4jUtils neo4jUtils,
            @Assisted URI uri
    ) {
        this(
                neo4jUtils.getOrCreate(uri)
        );
    }

    @AssistedInject
    protected Neo4jFriendlyResource(
            Neo4jUtils neo4jUtils,
            @Assisted URI uri,
            @Assisted String label
    ) {
        this(
                neo4jUtils.getOrCreate(uri)
        );
        label(label);
    }

    @AssistedInject
    protected Neo4jFriendlyResource(
            Neo4jUtils neo4jUtils,
            @Assisted JSONObject json
    ) {
        this(
                neo4jUtils.getOrCreate(
                        Uris.get(json.optString(
                                FriendlyResourceJson.URI
                        )
                        )
                )
        );
        label(
                json.optString(
                        FriendlyResourceJson.LABEL
                )
        );
        comment(
                json.optString(
                        FriendlyResourceJson.COMMENT
                )
        );
    }

    @Override
    public URI uri() {
        return Uris.get(
                node.getProperty(
                        Neo4jUserGraph.URI_PROPERTY_NAME).toString()
        );
    }

    @Override
    public boolean hasLabel() {
        return !label().isEmpty();
    }

    public String label() {
        return node.hasProperty(
                RDFS.label.getURI()
        ) ?
                node.getProperty(
                        RDFS.label.getURI()
                ).toString() :
                "";
    }

    @Override
    public void label(String label) {
        node.setProperty(
                RDFS.label.getURI(),
                label
        );
        updateLastModificationDate();
    }

    @Override
    public Set<Image> images() {
        return imageUtils.getImages(node);
    }

    @Override
    public Boolean gotImages() {
        return images().size() > 0;
    }

    @Override
    public String comment() {
        return node.hasProperty(
                RDFS.comment.getURI()
        ) ?
                node.getProperty(
                        RDFS.comment.getURI()
                ).toString() :
                "";
    }

    @Override
    public void comment(String comment) {
        node.setProperty(
                RDFS.comment.getURI(),
                comment
        );
        updateLastModificationDate();
    }

    @Override
    public Boolean gotComments() {
        return !StringUtils.isEmpty(
                comment()
        );
    }

    @Override
    public void addImages(Set<Image> images) {
        imageUtils.addImages(
                node,
                images
        );
    }

    @Override
    public DateTime creationDate() {
        return new DateTime((Long) node.getProperty(
                CREATION_DATE_PROPERTY_NAME
        ));
    }

    @Override
    public DateTime lastModificationDate() {
        return new DateTime((Long) node.getProperty(
                LAST_MODIFICATION_DATE_PROPERTY_NAME
        ));
    }

    public void updateLastModificationDate(){
        node.setProperty(
                LAST_MODIFICATION_DATE_PROPERTY_NAME,
                new Date().getTime()
        );
    }

    public Node getNode(){
        return node;
    }

    @Override
    public boolean equals(Object friendlyResourceToCompareAsObject) {
        FriendlyResource friendlyResourceToCompare = (FriendlyResource) friendlyResourceToCompareAsObject;
        return uri().equals(friendlyResourceToCompare.uri());
    }

    @Override
    public int hashCode() {
        return uri().hashCode();
    }

    private boolean hasCreationDate(){
        return node.hasProperty(
                CREATION_DATE_PROPERTY_NAME
        );
    }

    private void initCreationAndLastModificationDate(){
        Long creationDate = new Date().getTime();
        node.setProperty(
                CREATION_DATE_PROPERTY_NAME,
                creationDate
        );
        node.setProperty(
                LAST_MODIFICATION_DATE_PROPERTY_NAME,
                creationDate
        );
    }
}
