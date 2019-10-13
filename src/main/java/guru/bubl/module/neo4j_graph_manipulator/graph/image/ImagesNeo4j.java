/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.image;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.json.ImageJson;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import static org.neo4j.driver.v1.Values.parameters;

public class ImagesNeo4j {

    public enum props {
        images
    }

    protected FriendlyResourceNeo4j friendlyResource;

    @Inject
    protected Driver driver;

    @AssistedInject
    public ImagesNeo4j(
            @Assisted FriendlyResourceNeo4j friendlyResource
    ) {
        this.friendlyResource = friendlyResource;
    }

    public void addAll(Set<Image> images) {
        Set<Image> current = get();
        current.addAll(images);
        try (Session session = driver.session()) {
            session.run(
                    friendlyResource.queryPrefix() + "SET n.images=$image",
                    parameters(
                            "uri", friendlyResource.uri().toString(),
                            "image", ImageJson.toJsonArray(current)
                    )
            );
        }
    }

    public Set<Image> get() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    friendlyResource.queryPrefix() + "RETURN n.images as images",
                    parameters(
                            "uri", friendlyResource.uri().toString()
                    )
            ).single();
            return record.get("images").asObject() == null ? new HashSet<Image>() : ImageJson.fromJson(
                    record.get("images").asString()
            );
        }
    }

}
