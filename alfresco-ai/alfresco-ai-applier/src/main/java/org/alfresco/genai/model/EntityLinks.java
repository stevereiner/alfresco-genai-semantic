package org.alfresco.genai.model;

import java.util.List;

/**
 * The {@code EntityLinks} class represents the result of entity linking a document using AI services.
 * It contains the all the entity links textS.
 *
 * <p>This class follows the builder pattern, allowing for a fluent and readable way to construct instances.
 *
 */
public class EntityLinks {

    /**
     * The entity links text content for the document.
     */
    private List<String> entityLinks;

    /**
     * The target of the entity links (wikidata or dbpedia).
     */
    private String target;

  
    /**
     * Gets the entity links text for of the document.
     *
     * @return The entity links text.
     */
    public List<String>  getEntityLinks() {
        return entityLinks;
    }

    /**
     * Sets the entity links text content for the document.
     *
     * @param entityLinks The entity links text.
     * @return This {@code EntityLinks} instance for method chaining.
     */
    public EntityLinks entityLinks(List<String> entityLinks) {
        this.entityLinks = entityLinks;
        return this;
    }
    
    
    /**
     * Gets the target of the entity links (wikidata or dbpedia)..
     *
     * @return The entity link target.
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the target of the entity links (wikidata or dbpedia)..
     *
     * @param target The target of the entity links
     * @return This {@code EntityLinks} instance for method chaining.
     */
    public EntityLinks target(String target) {
        this.target = target;
        return this;
    }
    



}
