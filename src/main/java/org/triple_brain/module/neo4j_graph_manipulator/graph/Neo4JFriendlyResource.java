package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.json.ExternalResourceJson;

import javax.inject.Inject;
import java.net.URI;
import java.util.Set;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JFriendlyResource implements FriendlyResource {
    @Inject
    Neo4JImageUtils imageUtils;

    @Inject
    Neo4JUtils neo4JUtils;

    private Node node;

    @AssistedInject
    protected Neo4JFriendlyResource(
            @Assisted Node node
    ) {
        this.node = node;
    }

    @AssistedInject
    protected Neo4JFriendlyResource(
            Neo4JUtils neo4JUtils,
            @Assisted URI uri
    ) {
        this(
                neo4JUtils.getOrCreate(uri)
        );
    }

    @AssistedInject
    protected Neo4JFriendlyResource(
            Neo4JUtils neo4JUtils,
            @Assisted URI uri,
            @Assisted String label
    ) {
        this(
                neo4JUtils.getOrCreate(uri)
        );
        label(label);
    }

    @AssistedInject
    protected Neo4JFriendlyResource(
            Neo4JUtils neo4JUtils,
            @Assisted JSONObject json
    ) {
        this(
                neo4JUtils.getOrCreate(
                        Uris.get(json.optString(
                                ExternalResourceJson.URI
                        )
                        )
                )
        );
        label(
                json.optString(
                        ExternalResourceJson.LABEL
                )
        );
        description(
                json.optString(
                        ExternalResourceJson.DESCRIPTION
                )
        );
    }

    @Override
    public URI uri() {
        return Uris.get(
                node.getProperty(
                        Neo4JUserGraph.URI_PROPERTY_NAME).toString()
        );
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
    }

    @Override
    public Set<Image> images() {
        return imageUtils.getImages(node);
    }

    @Override
    public Boolean gotTheImages() {
        return images().size() > 0;
    }

    @Override
    public String description() {
        return node.hasProperty(
                RDFS.comment.getURI()
        ) ?
                node.getProperty(
                        RDFS.comment.getURI()
                ).toString() :
                "";
    }

    @Override
    public void description(String description) {
        node.setProperty(
                RDFS.comment.getURI(),
                description
        );
    }

    @Override
    public Boolean gotADescription() {
        return !StringUtils.isEmpty(
                description()
        );
    }

    @Override
    public void addImages(Set<Image> images) {
        imageUtils.addImages(
                node,
                images
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
}
