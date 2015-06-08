/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.embedded;

import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.rest.graphdb.RequestResult;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.batch.BatchCallback;
import org.neo4j.rest.graphdb.batch.CypherResult;
import org.neo4j.rest.graphdb.converter.RestEntityExtractor;
import org.neo4j.rest.graphdb.entity.RestEntity;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.entity.RestRelationship;
import org.neo4j.rest.graphdb.index.IndexInfo;
import org.neo4j.rest.graphdb.index.RestIndex;
import org.neo4j.rest.graphdb.index.RestIndexManager;
import org.neo4j.rest.graphdb.services.RequestType;
import org.neo4j.rest.graphdb.traversal.RestTraverser;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.neo4j.rest.graphdb.util.ResultConverter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


public class RestApiUsingEmbedded implements RestAPI {

    private GraphDatabaseService graphDb;
    private ExecutionEngine engine;

    public static RestApiUsingEmbedded usingGraphDb(GraphDatabaseService graphDb) {
        return new RestApiUsingEmbedded(
                graphDb
        );
    }

    protected RestApiUsingEmbedded(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
        engine = new ExecutionEngine(
                graphDb,
                StringLogger.SYSTEM
        );
    }

    @Override
    public RestIndexManager index() {
        return null;
    }

    @Override
    public RestNode getNodeById(long l) {
        return null;
    }

    @Override
    public RestRelationship getRelationshipById(long l) {
        return null;
    }

    @Override
    public RestNode createNode(Map<String, Object> stringObjectMap) {
        return null;
    }

    @Override
    public RestNode createRestNode(RequestResult requestResult) {
        return null;
    }

    @Override
    public RestRelationship createRelationship(Node node, Node node2, RelationshipType relationshipType, Map<String, Object> stringObjectMap) {
        return null;
    }

    @Override
    public RestRelationship createRestRelationship(RequestResult requestResult, PropertyContainer propertyContainer) {
        return null;
    }

    @Override
    public <T extends PropertyContainer> RestIndex<T> getIndex(String s) {
        return null;
    }

    @Override
    public void createIndex(String s, String s2, Map<String, String> stringStringMap) {

    }

    @Override
    public TraversalDescription createTraversalDescription() {
        return null;
    }

    @Override
    public Node getReferenceNode() {
        return null;
    }

    @Override
    public Transaction beginTx() {
        return graphDb.beginTx();
    }

    @Override
    public <S extends PropertyContainer> IndexHits<S> queryIndex(String s, Class<S> sClass) {
        return null;
    }

    @Override
    public void deleteEntity(RestEntity restEntity) {

    }

    @Override
    public IndexInfo indexInfo(String s) {
        return null;
    }

    @Override
    public void setPropertyOnEntity(RestEntity restEntity, String s, Object o) {

    }

    @Override
    public Map<String, Object> getPropertiesFromEntity(RestEntity restEntity) {
        return null;
    }

    @Override
    public void delete(RestIndex restIndex) {

    }

    @Override
    public <T extends PropertyContainer> void removeFromIndex(RestIndex restIndex, T t, String s, Object o) {

    }

    @Override
    public <T extends PropertyContainer> void removeFromIndex(RestIndex restIndex, T t, String s) {

    }

    @Override
    public <T extends PropertyContainer> void removeFromIndex(RestIndex restIndex, T t) {

    }

    @Override
    public <T extends PropertyContainer> void addToIndex(T t, RestIndex restIndex, String s, Object o) {

    }

    @Override
    public <T extends PropertyContainer> T putIfAbsent(T t, RestIndex restIndex, String s, Object o) {
        return null;
    }

    @Override
    public Map<?, ?> getData(RestEntity restEntity) {
        return null;
    }

    @Override
    public boolean hasToUpdate(long l) {
        return false;
    }

    @Override
    public void removeProperty(RestEntity restEntity, String s) {

    }

    @Override
    public CypherResult query(String s, Map<String, Object> stringObjectMap) {
        ExecutionResult result = engine.execute(
                s,
                stringObjectMap
        );
        return null;
    }

    @Override
    public Iterable<Relationship> getRelationships(RestNode restNode, String s) {
        return null;
    }

    @Override
    public RestTraverser traverse(RestNode restNode, Map<String, Object> stringObjectMap) {
        return null;
    }

    @Override
    public String getBaseUri() {
        return null;
    }

    @Override
    public <T> T getPlugin(Class<T> tClass) {
        return null;
    }

    @Override
    public <T> T getService(Class<T> tClass, String s) {
        return null;
    }

    @Override
    public <T> T executeBatch(BatchCallback<T> batchCallback) {
        try {
            T batchResult = batchCallback.recordBatch(this);
            return batchResult;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public RestNode getOrCreateNode(RestIndex<Node> nodeRestIndex, String s, Object o, Map<String, Object> stringObjectMap) {
        return null;
    }

    @Override
    public RestRelationship getOrCreateRelationship(RestIndex<Relationship> relationshipRestIndex, String s, Object o, RestNode restNode, RestNode restNode2, String s2, Map<String, Object> stringObjectMap) {
        return null;
    }

    @Override
    public QueryResult<Map<String, Object>> query(String s, Map<String, Object> stringObjectMap, ResultConverter resultConverter) {
        return null;
    }

    @Override
    public QueryResult<Object> run(String s, Map<String, Object> stringObjectMap, ResultConverter resultConverter) {
        return null;
    }

    @Override
    public RestEntityExtractor createExtractor() {
        return null;
    }

    @Override
    public <T extends PropertyContainer> RestIndex<T> createIndex(Class<T> tClass, String s, Map<String, String> stringStringMap) {
        return null;
    }

    @Override
    public RequestResult execute(RequestType requestType, String s, Object o) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isAutoIndexingEnabled(Class<? extends PropertyContainer> aClass) {
        return false;
    }

    @Override
    public void setAutoIndexingEnabled(Class<? extends PropertyContainer> aClass, boolean b) {

    }

    @Override
    public Set<String> getAutoIndexedProperties(Class aClass) {
        return null;
    }

    @Override
    public void startAutoIndexingProperty(Class aClass, String s) {

    }

    @Override
    public void stopAutoIndexingProperty(Class aClass, String s) {

    }

    @Override
    public void addLabels(RestNode restNode, String... strings) {

    }

    @Override
    public void removeLabel(RestNode restNode, String s) {

    }

    @Override
    public Collection<String> getNodeLabels(String s) {
        return null;
    }

    @Override
    public Collection<String> getAllLabelNames() {
        return null;
    }

    @Override
    public Iterable<RestNode> getNodesByLabel(String s) {
        return null;
    }

    @Override
    public Iterable<RestNode> getNodesByLabelAndProperty(String s, String s2, Object o) {
        return null;
    }
}
