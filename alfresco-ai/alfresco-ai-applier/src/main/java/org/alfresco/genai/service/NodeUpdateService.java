package org.alfresco.genai.service;

import org.alfresco.core.handler.NodesApi;
import org.alfresco.core.handler.TagsApi;
import org.alfresco.core.model.NodeBodyUpdate;
import org.alfresco.core.model.TagBody;
import org.alfresco.genai.model.Description;
import org.alfresco.genai.model.EntityLinks;
import org.alfresco.genai.model.Summary;
import org.alfresco.genai.model.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code NodeUpdateService} class is a Spring service responsible for updating document nodes with GenAI
 * information. It utilizes the Alfresco Nodes API and Tags API for updating node properties and creating tags
 * associated with the document identified by its UUID.
 */
@Service
public class NodeUpdateService {

    static final Logger LOG = LoggerFactory.getLogger(NodeUpdateService.class);
	
    /**
     * Constant representing the property name for tags.
     */
    static final String TAG_PROPERTY = "TAG";

    /**
     * The aspect name to be applied to a summarized document.
     */
    @Value("${content.service.summary.aspect}")
    String summaryAspect;

    /**
     * The property name for storing the document summary in the Alfresco repository obtained from configuration.
     */
    @Value("${content.service.summary.summary.property}")
    String summaryProperty;

    /**
     * The property name for storing the document tags in the Alfresco repository obtained from configuration.
     */
    @Value("${content.service.summary.tags.property}")
    String summaryTagsProperty;

    /**
     * The property name for storing the document model information in the Alfresco repository obtained from configuration.
     */
    @Value("${content.service.summary.model.property}")
    String summaryModelProperty;

    /**
     * Aspect name associated with document classification.
     */
    @Value("${content.service.classify.aspect}")
    private String classifyAspect;

    /**
     * The property name for storing the term content in the Alfresco repository obtained from configuration.
     */
    @Value("${content.service.classify.term.property}")
    private String termProperty;

    /**
     * The property name for storing the answer model information in the Alfresco repository obtained from configuration.
     */
    @Value("${content.service.classify.model.property}")
    private String termModelProperty;

    /**
     * Aspect name associated with picture description.
     */
    @Value("${content.service.description.aspect}")
    private String descriptionAspect;

    /**
     * The property name for storing the description content in the Alfresco repository obtained from configuration.
     */
    @Value("${content.service.description.description.property}")
    private String descriptionProperty;

    /**
     * The property name for storing the answer model information in the Alfresco repository obtained from configuration.
     */
    @Value("${content.service.description.model.property}")
    private String descriptionModelProperty;
  
    /**
     * Aspect name for storing Wikidata entity links data.
     */
    @Value("${content.service.entitylinks-wikidata.aspect}")
    private String entityLinksWikidataAspect;

    /**
     * The property name for storing  Wikidata entity links data in the Alfresco repository obtained from configuration.
     */
    @Value("${content.service.entitylinks-wikidata.linksWikidata.property}")
    private String linksWikidataProperty;

    /**
     * Aspect name for storing DBpedia entity links data.
     */
    @Value("${content.service.entitylinks-dbpedia.aspect}")
    private String entityLinksDBpediaAspect;
    
    /**
     * The property name for storing DBpedia entity links data in the Alfresco repository obtained from configuration.
     */
    @Value("${content.service.entitylinks-dbpedia.linksDBpedia.property}")
    private String linksDBpediaProperty;
    
    
    /**
     * Autowired instance of {@link NodesApi} for communication with the Alfresco Nodes API.
     */
    @Autowired
    NodesApi nodesApi;

    /**
     * Autowired instance of {@link TagsApi} for communication with the Alfresco Tags API.
     */
    @Autowired
    TagsApi tagsApi;

    /**
     * Updates the node properties and creates tags for the document identified by its UUID based on the provided
     * {@link Summary} object.
     *
     * @param uuid     The unique identifier of the document node.
     * @param summary  The {@link Summary} object containing summary, tags, and model information.
     */
    public void updateNodeSummary(String uuid, Summary summary) {

        List<String> aspectNames =
                nodesApi.getNode(uuid, null, null, null).getBody().getEntry().getAspectNames();
        if (!aspectNames.contains(summaryAspect)) {
            aspectNames.add(summaryAspect);
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(summaryProperty, summary.getSummary());
        if (!summaryModelProperty.equals(TAG_PROPERTY)) {
            properties.put(summaryModelProperty, summary.getModel());
        }
        if (!summaryTagsProperty.equals(TAG_PROPERTY)) {
            properties.put(summaryTagsProperty, summary.getTags());
        }
        nodesApi.updateNode(uuid,
                new NodeBodyUpdate().properties(properties).aspectNames(aspectNames),
                null, null);

        if (summaryModelProperty.equals(TAG_PROPERTY)) {
            tagsApi.createTagForNode(uuid, new TagBody().tag(summary.getModel()), null);
        }

        if (summaryTagsProperty.equals(TAG_PROPERTY)) {
            summary.getTags().forEach(tag ->
                    tagsApi.createTagForNode(uuid, new TagBody().tag(tag.replace('.', ' ').trim()), null));
        }
    }

    /**
     * Updates the node properties with term content and model information for the document identified by its UUID based
     * on the provided {@link Term} object.
     *
     * @param uuid  The unique identifier of the document node.
     * @param term  The {@link Term} object containing the answer content and model information.
     */
    public void updateNodeTerm(String uuid, Term term) {

        List<String> aspectNames =
                nodesApi.getNode(uuid, null, null, null).getBody().getEntry().getAspectNames();
        if (!aspectNames.contains(classifyAspect)) {
            aspectNames.add(classifyAspect);
        }

        nodesApi.updateNode(uuid,
                new NodeBodyUpdate()
                        .properties(Map.of(
                                termProperty, term.getTerm(),
                                termModelProperty, term.getModel()))
                        .aspectNames(aspectNames),
                null, null);
    }
    
    /**
     * Updates the node properties with description and model information for the document identified by its UUID based
     * on the provided {@link Description} object.
     *
     * @param uuid         The unique identifier of the picture node.
     * @param description  The {@link Description} object containing the answer content and model information.
     */
    public void updateNodeDescription(String uuid, Description description) {

        List<String> aspectNames =
                nodesApi.getNode(uuid, null, null, null).getBody().getEntry().getAspectNames();
        if (!aspectNames.contains(descriptionAspect)) {
            aspectNames.add(descriptionAspect);
        }

        nodesApi.updateNode(uuid,
                new NodeBodyUpdate()
                        .properties(Map.of(
                                descriptionProperty, description.getDescription(),
                                descriptionModelProperty, description.getModel()))
                        .aspectNames(aspectNames),
                null, null);
    }
    
    /**
     * Updates the node properties with entity links for the document identified by its UUID based
     * on the provided {@link EntityLinks} object.
     *
     * @param uuid         The unique identifier of the document node.
     * @param entityLinks  The {@link EntityLinks} object containing the entity links data.
     */
    public void updateNodeEntityLinksWikidata(String uuid, EntityLinks entityLinks) {

    	LOG.debug("ai-applier NodeUpdateService updateNodeEntityLinksWikidata");  	

    	List<String> aspectNames =
                nodesApi.getNode(uuid, null, null, null).getBody().getEntry().getAspectNames();
        if (!aspectNames.contains(entityLinksWikidataAspect)) {
            aspectNames.add(entityLinksWikidataAspect);
        }

        nodesApi.updateNode(uuid,
                new NodeBodyUpdate()
                        .properties(Map.of(
                                linksWikidataProperty, entityLinks.getEntityLinks() ))
                        .aspectNames(aspectNames),
                null, null);
    }

    /**
     * Updates the node properties with entity links for the document identified by its UUID based
     * on the provided {@link EntityLinks} object.
     *
     * @param uuid         The unique identifier of the document node.
     * @param entityLinks  The {@link EntityLinks} object containing the entity links data.
     */
    public void updateNodeEntityLinksDBpedia(String uuid, EntityLinks entityLinks) {

    	LOG.debug("ai-applier NodeUpdateService updateNodeEntityLinksDBpedia");   	
    	
        List<String> aspectNames =
                nodesApi.getNode(uuid, null, null, null).getBody().getEntry().getAspectNames();
        if (!aspectNames.contains(entityLinksDBpediaAspect)) {
            aspectNames.add(entityLinksDBpediaAspect);
        }

        nodesApi.updateNode(uuid,
                new NodeBodyUpdate()
                        .properties(Map.of(
                                linksDBpediaProperty, entityLinks.getEntityLinks() ))
                        .aspectNames(aspectNames),
                null, null);
    }    
    
}
