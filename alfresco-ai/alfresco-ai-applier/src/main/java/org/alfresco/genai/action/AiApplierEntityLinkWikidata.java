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
 * The {@code AiApplierWikidata} class is a Spring component that implements the {@link AiApplierAction} interface
 * for performing document Wikidata entity linking in the AI Applier application.
 */
@Component
public class AiApplierEntityLinkWikidata implements AiApplierAction {

    static final Logger LOG = LoggerFactory.getLogger(AiApplierEntityLinkWikidata.class);

    /**
     * Autowired instance of {@link NodesApi} for communication with the Alfresco Nodes API.
     */
    @Autowired
    NodesApi nodesApi;

    /**
     * The property names for storing the document Wikidata entity links in the Alfresco repository obtained from configuration.
     */
    @Value("${content.service.entitylinks-wikidata.labelsWikidata.property}")
    String entitylabelsWikidataProperty;

    @Value("${content.service.entitylinks-wikidata.linksWikidata.property}")
    String entitylinksWikidataProperty;

    @Value("${content.service.entitylinks-wikidata.typelistsWikidata.property}")
    String entitytypelistsWikidataProperty;

    
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
     * Executes the document entity links action on the given {@code ResultSetRowEntry}.
     *
     * @param entry The entry representing an Alfresco document for entity linking.
     * @return {@code true} if the entity linking was successful; otherwise, {@code false}.
     * @throws RuntimeException If an error occurs during entity linking, such as IO exception.
     */
    @Override
    public boolean execute(ResultSetRowEntry entry) {

        String uuid = entry.getEntry().getId();
        
        if (getMimeType(uuid).contains("image")) {
			LOG.debug("Document {} is an image, entity linking is not supported", entry.getEntry().getName());
			return false;
		}
        
        LOG.info("AiApplierEntityLinkWikidata Wkidata entity linking document {} ({})", entry.getEntry().getName(), uuid);

        if (renditionService.pdfRenditionIsCreated(uuid)) {

            try {

                nodeUpdateService.updateNodeEntityLinksWikidata(uuid, genAiClient.getEntityLinksWikidata(renditionService.getRenditionContent(uuid)));
                LOG.info("Document {} has been updated with all Wikidata entity links data in an apsect", entry.getEntry().getName());
                return true;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else {

            LOG.info("PDF rendition for document {} was not available, it has been requested", entry.getEntry().getName());
            renditionService.createPdfRendition(uuid);

        }

        return false;

    }

    /**
     * Returns the property name for storing the Wikidata entity linking data in the Alfresco repository.
     *
     * @return The name of the property used for storing Wikidata entity linking data in Alfresco.
     */
    @Override
    public String getUpdateField() {
        return entitylinksWikidataProperty;
    }
    
    public String getMimeType(String uuid) {
    	return nodesApi.getNode(uuid, null, null, null).getBody().getEntry().getContent().getMimeType();
    }        
}
