/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.*;
import org.triple_brain.module.model.json.IdentificationJson;
import org.triple_brain.module.neo4j_graph_manipulator.graph.*;

import javax.inject.Inject;
import java.net.URI;
import java.util.*;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class Neo4jGraphElementOperator implements GraphElementOperator, Neo4jOperator {

    public enum props{
        identifications
    }

    protected Node node;
    protected Neo4jIdentification identification;
    protected URI uri;
    protected QueryEngine<Map<String, Object>> queryEngine;
    protected RestAPI restApi;
    protected Neo4jIdentificationFactory identificationFactory;

    @AssistedInject
    protected Neo4jGraphElementOperator(
            QueryEngine queryEngine,
            RestAPI restApi,
            Neo4jIdentificationFactory identificationFactory,
            @Assisted Node node
    ) {
        identification = identificationFactory.withNode(
                node
        );
        this.identificationFactory = identificationFactory;
        this.node = node;
        this.queryEngine = queryEngine;
        this.restApi = restApi;
    }

    @AssistedInject
    protected Neo4jGraphElementOperator(
            QueryEngine queryEngine,
            RestAPI restApi,
            Neo4jIdentificationFactory identificationFactory,
            @Assisted URI uri
    ) {
        identification = identificationFactory.withUri(
                uri
        );
        this.identificationFactory = identificationFactory;
        this.queryEngine = queryEngine;
        this.restApi = restApi;
    }

    @Override
    public Date creationDate() {
        return identification.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return identification.lastModificationDate();
    }

    @Override
    public String getOwnerUsername() {
        return identification.getOwnerUsername();
    }

    public void updateLastModificationDate() {
        identification.updateLastModificationDate();
    }

    @Override
    public URI uri() {
        return identification.uri();
    }

    @Override
    public String label() {
        return identification.label();
    }

    @Override
    public void label(String label) {
        identification.label(
                label
        );
    }

    @Override
    public Set<Image> images() {
        return identification.images();
    }

    @Override
    public Boolean gotImages() {
        return identification.gotImages();
    }

    @Override
    public String comment() {
        return identification.comment();
    }

    @Override
    public void comment(String comment) {
        identification.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return identification.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        identification.addImages(
                images
        );
    }

    @Override
    public boolean hasLabel() {
        return identification.hasLabel();
    }

    @Override
    public IdentificationPojo addGenericIdentification(Identification genericIdentification) throws IllegalArgumentException {
        return addIdentificationUsingType(
                genericIdentification,
                IdentificationType.generic
        );
    }

    @Override
    public void create() {
        createUsingInitialValues(
                map()
        );
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        identification.createUsingInitialValues(values);
    }

    @Override
    public Map<URI, Identification> getGenericIdentifications() {
        return getIdentificationOfType(
                IdentificationType.generic
        );
    }

    @Override
    public IdentificationPojo addSameAs(Identification sameAs) throws IllegalArgumentException {
        return addIdentificationUsingType(
                sameAs,
                IdentificationType.same_as
        );
    }

    private IdentificationPojo addIdentificationUsingType(
            Identification identification,
            IdentificationType identificationType
    ) {
        ifIdentificationIsSelfThrowException(identification);
        Map<URI, IdentificationPojo> identifications = getIdentifications();

        IdentificationPojo identificationPojo = new IdentificationPojo(
                new UserUris(getOwnerUsername()).generateIdentificationUri(),
                identification
        );
        if(identifications.containsKey(identification.getExternalResourceUri())){
            identificationPojo.setUri(
                    identifications.get(
                            identification.getExternalResourceUri()
                    ).uri()
            );
        }
        identificationPojo.setCreationDate(new Date());
        identificationPojo.setType(identificationType);
        identifications.put(
                identificationPojo.getExternalResourceUri(),
                identificationPojo
        );
        setIdentifications(identifications);
        return identificationPojo;
    }

    @Override
    public Map<URI, Identification> getSameAs() {
        return getIdentificationOfType(
                IdentificationType.same_as
        );
    }

    @Override
    public IdentificationPojo addType(Identification type) throws IllegalArgumentException {
        return addIdentificationUsingType(
                type,
                IdentificationType.type
        );
    }

    @Override
    public void removeIdentification(Identification identification) {
        Map<URI, IdentificationPojo> identifications = getIdentifications();
        identifications.remove(identification.getExternalResourceUri());
        identifications.values().remove(identification);
        setIdentifications(identifications);
        updateLastModificationDate();
    }

    @Override
    public Map<URI, Identification> getAdditionalTypes() {
        return getIdentificationOfType(
                IdentificationType.type
        );
    }

    @Override
    public void remove() {
        identification.remove();
    }


    private Map<URI, Identification> getIdentificationOfType(IdentificationType identificationType) {
        Map<URI, Identification> identificationsOfType = new HashMap<>();
        for(IdentificationPojo identification: getIdentifications().values()){
            if(identification.getType().equals(identificationType)){
                identificationsOfType.put(
                        identification.getExternalResourceUri(),
                        identification
                );
            }
        }
        return identificationsOfType;
    }

    private void ifIdentificationIsSelfThrowException(Identification identification) throws IllegalArgumentException {
        if (identification.getExternalResourceUri().equals(this.uri())) {
            throw new IllegalArgumentException(
                    "identification cannot be the same"
            );
        }
    }

    @Override
    public boolean equals(Object graphElementToCompare) {
        return identification.equals(graphElementToCompare);
    }

    @Override
    public int hashCode() {
        return identification.hashCode();
    }

    @Override
    public String queryPrefix() {
        return identification.queryPrefix();
    }

    @Override
    public Node getNode() {
        if (null == node) {
            node = identification.getNode();
        }
        return node;
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        return identification.addCreationProperties(
                map
        );
    }

    @Override
    public URI getExternalResourceUri() {
        return identification.getExternalResourceUri();
    }

    @Override
    public Map<URI, IdentificationPojo> getIdentifications(){
        QueryResult<Map<String, Object>> result = queryEngine.query(
                identification.queryPrefix() +
                        "return n." + props.identifications + " as identifications",
                map()
        );
        if(!result.iterator().hasNext()){
            return new HashMap<>();
        }
        Object identificationsValue = result.iterator().next().get("identifications");
        if(identificationsValue == null){
            return new HashMap<>();
        }
        return IdentificationJson.fromJson(
                identificationsValue.toString()
        );
    }

    private void setIdentifications(Map<URI, IdentificationPojo> identifications){
        queryEngine.query(
                this.identification.queryPrefix() +
                        "SET n." + props.identifications + "= { " + props.identifications + "} ",
                map(
                        props.identifications.name(), IdentificationJson.toJson(identifications)
                )
        );
    }
}
