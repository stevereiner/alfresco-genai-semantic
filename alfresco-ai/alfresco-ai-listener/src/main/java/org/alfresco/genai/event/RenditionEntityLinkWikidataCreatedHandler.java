package org.alfresco.genai.event;

import org.alfresco.core.handler.NodesApi;
import org.alfresco.event.sdk.handling.filter.EventFilter;
import org.alfresco.event.sdk.handling.filter.NodeTypeFilter;
import org.alfresco.event.sdk.handling.handler.OnNodeCreatedEventHandler;
import org.alfresco.event.sdk.model.v1.model.DataAttributes;
import org.alfresco.event.sdk.model.v1.model.NodeResource;
import org.alfresco.event.sdk.model.v1.model.RepoEvent;
import org.alfresco.event.sdk.model.v1.model.Resource;
import org.alfresco.genai.service.GenAiClient;
import org.alfresco.genai.service.NodeUpdateService;
import org.alfresco.genai.service.RenditionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * The {@code RenditionEntityLinkWikidata CreatedHandler} class is a Spring component responsible for handling events triggered upon the
 * creation of nodes, specifically focusing on PDF renditions. It implements the {@link OnNodeCreatedEventHandler}
 * interface to define custom logic for processing node creation events.
 *
 * <p>When a PDF rendition is created, this handler checks if the associated document has a specified entitylinks aspect.
 * If the condition is met, the handler initiates the process of Wikidata entity linking the document by using the Spacy NLP / GenAI service and
 * updates the document node with the obtained entity links
 *
 */
@Component
public class RenditionEntityLinkWikidataCreatedHandler implements OnNodeCreatedEventHandler {

    /**
     * Logger for logging information and error messages.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RenditionEntityLinkWikidataCreatedHandler.class);

    /**
     * Aspect name associated with document entitylinks.
     */
    @Value("${content.service.entitylinks-wikidata.aspect}")
    private String entityLinksWikidataAspect;

    /**
     * Autowired instance of {@link GenAiClient} for interacting with the GenAI service.
     */
    @Autowired
    GenAiClient genAiClient;

    /**
     * Autowired instance of {@link NodesApi} for working with Alfresco nodes.
     */
    @Autowired
    NodesApi nodesApi;

    /**
     * Autowired instance of {@link RenditionService} for handling document renditions.
     */
    @Autowired
    RenditionService renditionService;

    /**
     * Autowired instance of {@link NodeUpdateService} for updating document nodes.
     */
    @Autowired
    NodeUpdateService nodeUpdateService;

    /**
     * Handles the node creation event triggered by the system. Checks for PDF renditions associated with documents
     * having the specified entitylinks aspect and initiates the document entity linking process.
     *
     * @param repoEvent The event containing information about the created node.
     */
    @Override
    public void handleEvent(final RepoEvent<DataAttributes<Resource>> repoEvent) {

    	LOG.info("ai-listener RenditionEntityLinkWikidataCreatedHandler handleEvent");
    	
    	NodeResource nodeResource = (NodeResource) repoEvent.getData().getResource();
        String uuid = nodeResource.getPrimaryHierarchy().get(0);

        // TODO Improve this condition, as it will be executed for every PDF rendition in the system (!)
        if (nodeResource.getName().equals("pdf") &&
                nodesApi.getNode(uuid, null, null, null).getBody().getEntry().getAspectNames().contains(entityLinksWikidataAspect)) {
            LOG.info("Wikidata Entity linking document {}", uuid);
            try {
            	nodeUpdateService.updateNodeEntityLinksWikidata(uuid, genAiClient.getEntityLinksWikidata(renditionService.getRenditionContent(uuid)));                                
            } catch (IOException e) {
                LOG.error("Error updating document {}", uuid, e);
            }
            LOG.info("Document {} has been updated with Wikidata entity links", uuid);
        }
    }

    /**
     * Specifies the event filter to determine which node creation events this handler should process. In this case,
     * the filter is based on the node type "cm:thumbnail."
     *
     * @return An {@link EventFilter} representing the filter criteria for node creation events.
     */
    @Override
    public EventFilter getEventFilter() {
        return NodeTypeFilter.of("cm:thumbnail");
    }

}