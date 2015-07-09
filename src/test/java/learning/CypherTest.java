/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package learning;

import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.common_utils.Uris;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.RelationshipType;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class CypherTest extends Neo4jServerTestGeneric {

    enum relationships implements RelationshipType {
        source,
        destination
    }

    @Test
    public void can_set_properties_using_map_of_node_found_in_index() {
        URI uri = Uris.get(
                "/" + UUID.randomUUID()
        );
        assertFalse(
                nodeWithUriExists(uri)
        );
        createNodeWithUri(uri);
        assertTrue(
                nodeWithUriExists(uri)
        );
        String query = String.format(
                "START n=node:node_auto_index(\"uri:%s\") SET n.name={1}",
                uri.toString()
        );
        NoExRun.wrap(() -> {
            PreparedStatement statement = connection.prepareStatement(
                    query
            );
            statement.setString(1, "pomme");
            return statement.executeUpdate();
        }).get();
        assertTrue(
                nodeWithUriExists(uri)
        );
        PropertyContainer propertyContainer = getNodeWithUri(uri);
        assertTrue(propertyContainer.hasProperty("name"));
        String name = propertyContainer.getProperty("name").toString();
        assertThat(
                name,
                is("pomme")
        );
    }

    @Test
    public void to_not_specify_properties_to_update_deletes_unmentioned_properties() {
        URI uri = randomUri();
        createNodeWithUri(uri);
        assertTrue(
                nodeWithUriExists(uri)
        );
        NoExRun.wrap(
                () -> {
                    String query = String.format(
                            "START n=node:node_auto_index(\"uri:%s\") SET n = {1}",
                            uri.toString()
                    );
                    PreparedStatement stm = connection.prepareStatement(query);
                    Map<String, Object> props = map(
                            "name", "pomme"
                    );
                    stm.setObject(
                            1,
                            props
                    );
                    return stm.execute();
                }
        ).get();
        assertFalse(
                nodeWithUriExists(uri)
        );
    }

    @Test
    public void creating_a_node_and_relations_with_existing_nodes_using_index() {
        URI middleNodeUri = randomUri();
        URI startNodeUri = randomUri();
        URI endNodeUri = randomUri();
        createNodeWithUri(startNodeUri);
        createNodeWithUri(endNodeUri);
        assertFalse(
                nodeWithUriExists(middleNodeUri)
        );
        NoExRun.wrap(
                () -> {
                    String query = String.format(
                            "START source_node=node:node_auto_index(\"uri:%s\"), " +
                                    "destination_node=node:node_auto_index(\"uri:%s\") " +
                                    "create (n {1}), " +
                                    "n-[:source]->source_node, " +
                                    "n-[:destination]->destination_node",
                            startNodeUri.toString(),
                            endNodeUri.toString()
                    );
                    PreparedStatement statement = connection.prepareStatement(
                            query
                    );
                    statement.setObject(
                            1,
                            map(
                                    "uri",
                                    middleNodeUri.toString()
                            )
                    );
                    return statement.execute();
                }
        ).get();
        assertTrue(
                nodeWithUriExists(middleNodeUri)
        );
        Node middleNode = getNodeWithUri(middleNodeUri);
        assertTrue(
                middleNode.hasRelationship(relationships.source)
        );
        assertTrue(
                middleNode.hasRelationship(relationships.destination)
        );
        Node startNodeFromRelationship = middleNode.getRelationships(
                Direction.OUTGOING,
                relationships.source
        ).iterator().next().getEndNode();
        assertThat(
                startNodeFromRelationship,
                is(getNodeWithUri(startNodeUri))
        );
        Node endNodeFromRelationship = middleNode.getRelationships(
                Direction.OUTGOING,
                relationships.destination
        ).iterator().next().getEndNode();
        assertThat(
                endNodeFromRelationship,
                is(getNodeWithUri(endNodeUri))
        );
    }

    @Test
    public void can_remove() {
        URI startNodeUri = randomUri();
        createNodeWithUri(startNodeUri);
        assertTrue(
                nodeWithUriExists(startNodeUri)
        );
        NoExRun.wrap(
                () -> {
                    String query = String.format(
                            "START " +
                                    "n=node:node_auto_index(\"uri:%s\") " +
                                    "DELETE " +
                                    "n",
                            startNodeUri.toString()
                    );
                    Statement stm = connection.createStatement();
                    return stm.executeQuery(query);
                }
        ).get();
        assertFalse(
                nodeWithUriExists(startNodeUri)
        );
    }

    @Test
    public void merging_existing_node_does_not_modify_its_property_value() {
        URI uri = randomUri();
        createNodeWithUri(uri);
        getNodeWithUri(uri).setProperty(
                "pomme",
                "avion"
        );
        NoExRun.wrap(
                () -> {
                    String query = "MERGE (f {" +
                            "uri: {1} " +
                            "}) " +
                            "ON CREATE " +
                            "SET f.pomme = {2} " +
                            "SET f.bonjour = {3} " +
                            "RETURN f";
                    PreparedStatement stm = connection.prepareStatement(query);
                    stm.setString(
                            1,
                            uri.toString()
                    );
                    stm.setString(
                            2,
                            "suspense"
                    );
                    stm.setString(
                            3,
                            "ivan"
                    );
                    return stm.execute();
                }
        ).get();
        Node node = getNodeWithUri(uri);
        assertTrue(
                node.hasProperty("bonjour")
        );
        assertThat(
                node.getProperty("pomme").toString(),
                is("avion")
        );
    }

    @Test
    public void can_merge_multiple_nodes() {
        URI uri1 = randomUri(),
                uri2 = randomUri();
        assertFalse(
                nodeWithUriExists(uri1)
        );
        assertFalse(
                nodeWithUriExists(uri2)
        );
        NoExRun.wrap(
                () -> {
                    String query = "MERGE (f {" +
                            "uri: {1} " +
                            "}) " +
                            "ON CREATE " +
                            "SET f.property = {2} " +
                            "MERGE (z {" +
                            "uri: {3} " +
                            "}) " +
                            "ON CREATE " +
                            "SET z.property = {4} " +
                            "RETURN f, z";
                    PreparedStatement stm = connection.prepareStatement(
                            query
                    );
                    stm.setString(1, uri1.toString());
                    stm.setString(2, "property1");
                    stm.setString(3, uri2.toString());
                    stm.setString(4, "property2");
                    return stm.execute();
                }
        ).get();
        assertTrue(
                nodeWithUriExists(uri1)
        );
        assertTrue(
                nodeWithUriExists(uri2)
        );
    }

    private Boolean nodeWithUriExists(URI uri) {
        return NoExRun.wrap(() -> {
            Statement stm = connection.createStatement();
            ResultSet rs = stm.executeQuery(
                    "START n=node:node_auto_index('uri:" + uri + "') "
                            + "return n.uri"
            );
            return rs.next();
        }).get();
    }

    private Node getNodeWithUri(URI uri) {
        return NoExRun.wrap(
                () -> {
                    String query = "START n=node:node_auto_index(\"uri:" + uri.toString() + "\") return n";
                    Statement stm = connection.createStatement();
                    ResultSet rs = stm.executeQuery(query);
                    rs.next();
                    return (Node) rs.getObject("n");
                }
        ).get();
    }

    private void createNodeWithUri(URI uri) {
        NoExRun.wrap(
                () -> {
                    String query = String.format(
                            "CREATE (n {%s:{1}})",
                            Neo4jUserGraph.URI_PROPERTY_NAME
                    );
                    PreparedStatement stm = connection.prepareStatement(
                            query
                    );
                    stm.setString(1, uri.toString());
                    return stm.executeUpdate();
                }
        ).get();
    }

    private URI randomUri() {
        return Uris.get(
                "/" + UUID.randomUUID()
        );
    }

}
