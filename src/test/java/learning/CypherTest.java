/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package learning;

import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.rest.graphdb.util.QueryResult;
import guru.bubl.module.common_utils.Uris;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;
import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.wrap;

public class CypherTest extends Neo4jServerTestGeneric {

    enum relationships implements RelationshipType {
        source,
        destination
    }

    @Test
    public void can_set_properties_using_map_of_node_found_in_index() {
        removeWholeGraph();
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
        Map<String, Object> props = map(
                "name", "pomme"
        );
        queryEngine.query(
                "START n=node:node_auto_index(\"uri:" + uri + "\") SET n.name= {name}",
                props
        );
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
        removeWholeGraph();
        URI uri = randomUri();
        createNodeWithUri(uri);
        assertTrue(
                nodeWithUriExists(uri)
        );
        Map<String, Object> props = map(
                "name", "pomme"
        );
        queryEngine.query(
                "START n=node:node_auto_index(\"uri:" + uri + "\") SET n = {props}",
                wrap(props)
        );
        assertFalse(
                nodeWithUriExists(uri)
        );
    }

    @Test
    public void creating_a_node_and_relations_with_existing_nodes_using_index() {
        removeWholeGraph();
        URI middleNodeUri = randomUri();
        URI startNodeUri = randomUri();
        URI endNodeUri = randomUri();
        createNodeWithUri(startNodeUri);
        createNodeWithUri(endNodeUri);
        Map<String, Object> props = map(
                "uri",
                middleNodeUri.toString()
        );
        String query =
                "START source_node=node:node_auto_index(\"uri:" + startNodeUri + "\"), " +
                        "destination_node=node:node_auto_index(\"uri:" + endNodeUri + "\") " +
                        "create (n {props}), " +
                        "n-[:source]->source_node, " +
                        "n-[:destination]->destination_node";
        assertFalse(
                nodeWithUriExists(middleNodeUri)
        );
        queryEngine.query(
                query,
                wrap(props)
        );
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
        removeWholeGraph();
        URI startNodeUri = randomUri();
        createNodeWithUri(startNodeUri);
        assertTrue(
                nodeWithUriExists(startNodeUri)
        );
        queryEngine.query(
                "START " +
                        "n=node:node_auto_index(\"uri:" + startNodeUri + "\") " +
                        "DELETE " +
                        "n",
                map()
        );
        assertFalse(
                nodeWithUriExists(startNodeUri)
        );
    }

    @Test
    public void merging_existing_node_does_not_modify_its_property_value() {
        removeWholeGraph();
        URI uri = randomUri();
        createNodeWithUri(uri);
        getNodeWithUri(uri).setProperty(
                "pomme",
                "avion"
        );
        queryEngine.query(
                "MERGE (f {" +
                        "uri: {uri} " +
                        "}) " +
                        "ON CREATE " +
                        "SET f.pomme = {pomme} " +
                        "SET f.bonjour = {bonjour} " +
                        "RETURN f",
                map(
                        "uri", uri.toString(),
                        "pomme", "suspense",
                        "bonjour", "ivan"
                )
        );
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
        queryEngine.query(
                "MERGE (f {" +
                        "uri: {uri1} " +
                        "}) " +
                        "ON CREATE " +
                        "SET f.property = {property1} " +
                        "MERGE (z {" +
                        "uri: {uri2} " +
                        "}) " +
                        "ON CREATE " +
                        "SET z.property = {property2} " +
                        "RETURN f, z",
                map(
                        "uri1", uri1.toString(),
                        "uri2", uri2.toString(),
                        "property1", "property1",
                        "property2", "property2"
                )
        );
        assertTrue(
                nodeWithUriExists(uri1)
        );
        assertTrue(
                nodeWithUriExists(uri2)
        );
    }

    private Boolean nodeWithUriExists(URI uri) {
        return nodeIndex.get(
                "uri",
                uri
        ).iterator().hasNext();
    }

    private Node getNodeWithUri(URI uri) {
        QueryResult<Map<String, Object>> result = queryEngine.query(
                "START n=node:node_auto_index(\"uri:" + uri.toString() + "\") return n",
                map()
        );
        Iterator<Map<String, Object>> iterator = result.iterator();
        Map<String, Object> row = iterator.next();
        return (Node) row.get("n");
    }

    private void createNodeWithUri(URI uri) {
        queryEngine.query(
                "CREATE (n {props})",
                wrap(
                        map(
                                Neo4jUserGraph.URI_PROPERTY_NAME, uri.toString()
                        )
                )
        );
    }

    private URI randomUri() {
        return Uris.get(
                "/" + UUID.randomUUID()
        );
    }

    public void removeWholeGraph() {
        queryEngine.query(
                "START n = node(*), r=relationship(*) DELETE n, r;",
                Collections.EMPTY_MAP
        );
    }
}
