package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

import com.hp.hpl.jena.vocabulary.RDFS;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.graph.FriendlyResourcePojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import org.triple_brain.module.neo4j_graph_manipulator.graph.image.Neo4jImages;

import java.net.URI;
import java.util.*;

/*
* Copyright Mozilla Public License 1.1
*/
public class FriendlyResourceFromExtractorQueryRow {

    private Map<String, Object> row;
    private String nodeKey;
    private String imageKey;

    public static FriendlyResourceFromExtractorQueryRow usingRowAndNodeKey(
            Map<String, Object> row,
            String nodeKey
    ){
        return new FriendlyResourceFromExtractorQueryRow(
            row,
            nodeKey
        );
    }

    public static FriendlyResourceFromExtractorQueryRow usingRowAndPrefix(
            Map<String, Object> row,
            String nodeKey
    ){
        return new FriendlyResourceFromExtractorQueryRow(
                row,
                nodeKey
        );
    }

    protected FriendlyResourceFromExtractorQueryRow(Map<String,Object> row, String nodeKey){
        this.row = row;
        this.nodeKey = nodeKey;
        this.imageKey = nodeKey + "_image";
    }

    public FriendlyResourcePojo build(){
        FriendlyResourcePojo friendlyResource = init();
        update(friendlyResource);
        return friendlyResource;
    }

    public void update(FriendlyResource friendlyResource){
        if(hasImageInRow()){
            friendlyResource.images().add(
                    imageInRow()
            );
        }
    }

    private Boolean hasImageInRow(){
        return row.get(
                imageKey + "." + Neo4jImages.props.base64_for_small
        ) != null;
    }

    private Image imageInRow(){
        return Image.withBase64ForSmallAndUriForBigger(
                row.get(
                        imageKey + "." + Neo4jImages.props.base64_for_small
                ).toString(),
                URI.create(row.get(
                        imageKey + "." + Neo4jImages.props.url_for_bigger
                ).toString())
        );
    }

    private FriendlyResourcePojo init(){
        return new FriendlyResourcePojo(
                URI.create(
                        row.get(nodeKey + "." + Neo4jUserGraph.URI_PROPERTY_NAME).toString()
                ),
                getLabel(),
                new HashSet<Image>(),
                getComment(),
                getCreationDate(),
                getLastModificationDate()
        );
    }

    private String getLabel(){
        String labelKey = nodeKey + ".`" + RDFS.label.getURI().toString() + "`";
        return row.get(
                labelKey
        ) != null ? row.get(labelKey).toString() : "";
    }

    private String getComment(){
        String commmentKey = nodeKey + ".`" + RDFS.comment.getURI().toString() + "`";
        return row.get(
                commmentKey
        ) != null ? row.get(commmentKey).toString() : "";
    }

    private Date getLastModificationDate(){
        return new Date((Long) row.get(
                nodeKey + "." + Neo4jFriendlyResource.props.last_modification_date.name()
        ));
    }

    private Date getCreationDate(){
        return new Date((Long) row.get(
                nodeKey + "." + Neo4jFriendlyResource.props.creation_date.name()
        ));
    }
}
