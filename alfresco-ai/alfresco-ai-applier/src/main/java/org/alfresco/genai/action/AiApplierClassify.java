package org.alfresco.genai.action;

import org.alfresco.core.handler.NodesApi;
import org.alfresco.genai.service.GenAiClient;
import org.alfresco.genai.service.NodeUpdateService;
import org.alfresco.genai.service.RenditionService;
import org.alfresco.search.model.ResultSetRowEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * The {@code AiApplierClassify} class is a Spring component that implements the {@link AiApplierAction} interface
 * for performing document classification in the AI Applier application.
 */
@Component
public class AiApplierClassify implements AiApplierAction {

    static final Logger LOG = LoggerFactory.getLogger(AiApplierClassify.class);

    /**
     * Autowired instance of {@link NodesApi} for communication with the Alfresco Nodes API.
     */
    @Autowired
    NodesApi nodesApi;
    
    /**
     * The property name for storing the term content in the Alfresco repository obtained from configuration.
     */
    @Value("${content.service.classify.term.property}")
    private String termProperty;

    /**
     * List of terms, separated by comma, containing a set of terms to be selected for the document.
     */
    @Value("${applier.action.classify.term.list}")
    String termList;

    /**
     * Http client for interacting with the GenAI service
     */
    @Autowired
    GenAiClient genAiClient;

    /**
     * Http client for handling document renditions in Alfresco
     */
    @Autowired
    RenditionService renditionService;

    /**
     * Http client for updating Alfresco document nodes
     */
    @Autowired
    NodeUpdateService nodeUpdateService;

    /**
     * Executes the document classification action on the given {@code ResultSetRowEntry}.
     *
     * @param entry The entry representing an Alfresco document for classification.
     * @return {@code true} if the classification was successful; otherwise, {@code false}.
     * @throws RuntimeException If an error occurs during classification, such as IO exception.
     */
    @Override
    public boolean execute(ResultSetRowEntry entry) {

        String uuid = entry.getEntry().getId();

        if (getMimeType(uuid).contains("image")) {
			LOG.debug("Document {} is an image, classification is not supported", entry.getEntry().getName());
			return false;
		}
        
        LOG.debug("Selecting term for document {} ({})", entry.getEntry().getName(), uuid);

        if (renditionService.pdfRenditionIsCreated(uuid)) {

            try {
                nodeUpdateService.updateNodeTerm(uuid, genAiClient.getTerm(renditionService.getRenditionContent(uuid), termList));
                LOG.debug("Document {} has been updated with term and tag", entry.getEntry().getName());
                return true;
                
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else {

            LOG.debug("PDF rendition for document {} was not available, it has been requested", entry.getEntry().getName());
            renditionService.createPdfRendition(uuid);
        }

        return false;
    }

    /**
     * Returns the property name for storing the term content in the Alfresco repository.
     *
     * @return The name of the property used for storing term content in Alfresco.
     */
    @Override
    public String getUpdateField() {
        return termProperty;
    }

    public String getMimeType(String uuid) {
    	return nodesApi.getNode(uuid, null, null, null).getBody().getEntry().getContent().getMimeType();
    }    
    
}
