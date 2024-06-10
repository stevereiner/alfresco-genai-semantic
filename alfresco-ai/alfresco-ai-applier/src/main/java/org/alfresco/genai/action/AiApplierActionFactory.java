package org.alfresco.genai.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The {@code AiApplierActionFactory} class is a Spring component responsible for creating instances
 * of {@link AiApplierAction} based on the specified {@link AiApplierAction.Action}.
 */
@Component
public class AiApplierActionFactory {

    /**
     * Action that creates summaries for Alfresco documents.
     */
    @Autowired
    AiApplierSummary aiApplierSummary;

    /**
     * Action that selects a term from a list of terms for Alfresco documents.
     */
    @Autowired
    AiApplierClassify aiApplierClassify;

    /**
     * Action that creates summaries for Alfresco documents.
     */
    @Autowired
    AiApplierDescription aiApplierDescription;

    /**
     * Action that creates Wikidata entity links for Alfresco documents.
     */
    @Autowired
    AiApplierEntityLinkWikidata aiApplierEntityLinkWikidata;

    /**
     * Action that creates DBpedia entity links for Alfresco documents.
     */
    @Autowired
    AiApplierEntityLinkDBpedia aiApplierEntityLinkDBpedia;

    
    /**
     * Returns the appropriate {@link AiApplierAction} instance based on the specified action.
     *
     * @param action The type of AI action to be performed.
     * @return An instance of {@link AiApplierAction} corresponding to the specified action.
     * @throws RuntimeException If the specified action is not supported.
     */
    public AiApplierAction getAiApplierAction(AiApplierAction.Action action) {
        return switch (action) {
            case SUMMARY -> aiApplierSummary;
            case CLASSIFY -> aiApplierClassify;
            case DESCRIBE -> aiApplierDescription;
            case ENTITYLINKWIKIDATA -> aiApplierEntityLinkWikidata;
            case ENTITYLINKDBPEDIA -> aiApplierEntityLinkDBpedia;
            default -> throw new RuntimeException("Action " + action + " is not supported!");
        };
    }

}
