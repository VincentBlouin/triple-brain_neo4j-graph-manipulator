package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.Inject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.common_utils.Uris;
import org.triple_brain.module.model.Image;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jImageUtils {

    @Inject
    private Neo4jUtils utils;

    public static final String URL_FOR_SMALL_KEY = "url_for_small";
    public static final String URL_FOR_BIGGER_KEY = "url_for_bigger";

    public void addImages(Node node, Set<Image> imagesToAdd) {
        Set<Image> existingImages = getImages(node);
        for (Image image : imagesToAdd) {
            if (!existingImages.contains(image)) {
                addImage(
                        node,
                        image
                );
            }
        }
    }

    public void addImage(Node node, Image image) {
        Node nodeAsImage = utils.create(
                Uris.get(
                        "/image/" + UUID.randomUUID().toString()
                )
        );
        nodeAsImage.setProperty(
                URL_FOR_SMALL_KEY,
                image.urlForSmall().toString()
        );
        nodeAsImage.setProperty(
                URL_FOR_BIGGER_KEY,
                image.urlForBigger().toString()
        );
        node.createRelationshipTo(nodeAsImage, Relationships.HAS_IMAGE);
    }

    public Set<Image> getImages(Node nodeWithImages) {
        Set<Image> images = new HashSet<>();
        for (Relationship relationship : nodeWithImages.getRelationships(Relationships.HAS_IMAGE)) {
            images.add(
                    getImage(relationship.getEndNode())
            );
        }
        return images;
    }

    public Image getImage(Node imageAsNode) {
        return Image.withUriForSmallAndBigger(
                URI.create(
                        imageAsNode.getProperty(
                                URL_FOR_SMALL_KEY
                        ).toString()
                ),
                URI.create(
                        imageAsNode.getProperty(
                                URL_FOR_BIGGER_KEY
                        ).toString()
                )
        );
    }
}
