/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.FriendlyResource;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.FriendlyResourceOperator;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.notification.NotificationOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.UserGraphNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.ImageFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.ImagesNeo4j;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.neo4j.driver.Values.parameters;


public class FriendlyResourceNeo4j implements FriendlyResourceOperator, OperatorNeo4j {

    public enum props {
        uri,
        label,
        comment,
        creation_date,
        last_modification_date,
        owner
    }

    public static final String LAST_MODIFICATION_QUERY_PART = String.format(
            " n.%s=$%s ",
            props.last_modification_date,
            props.last_modification_date
    );

    public static Map<String, Object> addUpdatedLastModificationDate(Map<String, Object> map) {
        map.put(
                props.last_modification_date.name(), new Date().getTime()
        );
        return map;
    }

    protected URI uri;

    protected ImagesNeo4j images;

    protected Driver driver;

    public static Boolean haveElementWithUri(URI uri, Driver driver) {
        String query = "MATCH(n:Resource{uri:$uri}) RETURN n.uri as uri";
        try (Session session = driver.session()) {
            return session.run(
                    query,
                    parameters(
                            "uri",
                            uri.toString()
                    )
            ).hasNext();
        }
    }

    @AssistedInject
    protected FriendlyResourceNeo4j(
            ImageFactoryNeo4j imageFactory,
            Driver driver,
            @Assisted URI uri
    ) {
        this.images = imageFactory.forResource(this);
        this.driver = driver;
        if (StringUtils.isEmpty(uri.toString())) {
            throw new RuntimeException("uri for friendly resource is mandatory");
        }
        this.uri = uri;
    }

    @AssistedInject
    protected FriendlyResourceNeo4j(
            ImageFactoryNeo4j imageFactory,
            Driver driver,
            @Assisted FriendlyResourcePojo pojo
    ) {

        this.images = imageFactory.forResource(this);
        this.driver = driver;
        this.uri = pojo.uri();
        createUsingInitialValues(
                RestApiUtilsNeo4j.map(
                        props.label.toString(), pojo.label() == null ? "" : pojo.label(),
                        props.comment.toString(), pojo.comment() == null ? "" : pojo.comment()
                )
        );
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public boolean hasLabel() {
        return !label().isEmpty();
    }

    @Override
    public String label() {
        String query = String.format(
                "%sRETURN n.label as label",
                queryPrefix()
        );
        try (Session session = driver.session()) {
            Result rs = session.run(
                    query,
                    parameters(
                            "uri", uri.toString()
                    )
            );
            Record record = rs.single();
            return record.get("label").asObject() == null ?
                    "" : record.get("label").asString();
        }
    }

    @Override
    public void label(String label) {
        addUpdateNotifications("label");
        String query = String.format(
                "%s SET n.label=$label, %s",
                queryPrefix(),
                LAST_MODIFICATION_QUERY_PART
        );
        try (Session session = driver.session()) {
            session.run(
                    query,
                    parameters(
                            "uri",
                            uri.toString(),
                            "label",
                            label,
                            "last_modification_date",
                            new Date().getTime()
                    )
            );
        }
    }

    public void addUpdateNotifications(String action) {
        if (!lastModificationIsLongEnoughForUpdate()) {
            return;
        }
        try (Session session = driver.session()) {
            String query = "MATCH (ge:Resource{copied_from_uri:$uri}) WHERE ge.owner<>$owner " +
                    "CREATE (n:Resource:Notification { " +
                    "uri:'/service/users/' + ge.owner + '/notification/' + apoc.create.uuid()," +
                    "owner: ge.owner," +
                    "watchUri: $uri," +
                    "watchLabel : ge.label," +
                    "rootUri: ge.copied_root_uri," +
                    "action:$action," +
                    "creationDate:timestamp()" +
                    "})  ";
            session.run(
                    query,
                    parameters(
                            "uri", uri.toString(),
                            "owner", getOwnerUsername(),
                            "action", action
                    )
            );
        }
    }

    private Boolean lastModificationIsLongEnoughForUpdate() {
        DateTime lastModificationDate = new DateTime(lastModificationDate());
        return lastModificationDate.plusDays(1).isBefore(new DateTime());
    }

    @Override
    public Set<Image> images() {
        return images.get();
    }

    @Override
    public Boolean gotImages() {
        return images().size() > 0;
    }

    @Override
    public String comment() {
        String query = String.format(
                "%sRETURN n.comment as comment",
                queryPrefix()
        );
        try (Session session = driver.session()) {
            Record record = session.run(
                    query,
                    parameters(
                            "uri", uri.toString()
                    )
            ).single();
            return record.get("comment").asObject() == null ?
                    "" : record.get("comment").asString();
        }
    }

    @Override
    public void comment(String comment) {
        addUpdateNotifications("description");
        String query = String.format(
                "%sSET n.comment=$comment, %s",
                queryPrefix(),
                LAST_MODIFICATION_QUERY_PART
        );
        Map<String, Object> props = RestApiUtilsNeo4j.map(
                "comment", comment
        );
        addUpdatedLastModificationDate(props);
        try (Session session = driver.session()) {
            session.run(
                    query,
                    parameters(
                            "uri", uri.toString(),
                            "comment", comment,
                            "last_modification_date", new Date().getTime()
                    )
            );
        }
    }

    @Override
    public Boolean gotComments() {
        return !StringUtils.isEmpty(
                comment()
        );
    }

    @Override
    public void addImages(Set<Image> images) {
        this.images.addAll(images);
    }

    @Override
    public void create() {
        createUsingInitialValues(
                RestApiUtilsNeo4j.map()
        );
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        Map<String, Object> creationProps = addCreationProperties(
                values
        );
        try (Session session = driver.session()) {
            session.run(
                    "CREATE(n:Resource $creationProps)",
                    parameters(
                            "creationProps",
                            creationProps
                    )
            );
        }
    }

    public FriendlyResourcePojo pojoFromCreationProperties(Map<String, Object> creationProperties) {
        FriendlyResourcePojo friendlyResourcePojo = new FriendlyResourcePojo(
                uri
        );
        friendlyResourcePojo.setCreationDate(
                new Long(
                        creationProperties.get(
                                props.creation_date.name()
                        ).toString()
                )
        );
        friendlyResourcePojo.setLastModificationDate(
                new Long(
                        creationProperties.get(
                                props.last_modification_date.name()
                        ).toString()
                )
        );
        return friendlyResourcePojo;
    }

    @Override
    public void remove() {
        try (Session session = driver.session()) {
            session.run(
                    String.format(
                            "%s DETACH DELETE n",
                            queryPrefix()
                    ),
                    parameters(
                            "uri",
                            uri.toString()
                    )
            );
        }
    }

    @Override
    public void setColors(String colors) {
        try (Session session = driver.session()) {
            session.run(
                    queryPrefix() + "SET n.colors=$colors",
                    parameters(
                            "uri",
                            uri().toString(),
                            "colors",
                            colors
                    )
            );
        }
    }

    @Override
    public Date creationDate() {
        try (Session session = driver.session()) {
            return new Date(
                    session.run(
                            String.format(
                                    "%s RETURN n.creation_date as creationDate",
                                    queryPrefix()
                            ),
                            parameters(
                                    "uri",
                                    uri.toString()
                            )
                    ).single().get("creationDate").asLong()
            );
        }

    }

    @Override
    public Date lastModificationDate() {
        try (Session session = driver.session()) {
            return new Date(
                    session.run(
                            String.format(
                                    "%s RETURN n.last_modification_date as lastModificationDate",
                                    queryPrefix()
                            ),
                            parameters(
                                    "uri",
                                    uri.toString()
                            )
                    ).single().get("lastModificationDate").asLong()
            );
        }
    }

    @Override
    public String getColors() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    queryPrefix() + "RETURN n.colors as colors",
                    parameters(
                            "uri",
                            this.uri().toString()
                    )
            ).single();
            return record.get(
                    "colors"
            ).asObject() == null ? "" : record.get("colors").asString();
        }
    }


    public void updateLastModificationDate() {
        String query = queryPrefix() +
                " SET " +
                LAST_MODIFICATION_QUERY_PART;
        try (Session session = driver.session()) {
            session.run(
                    query,
                    parameters(
                            "uri",
                            uri.toString(),
                            "last_modification_date",
                            new Date().getTime()
                    )
            );
        }
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
