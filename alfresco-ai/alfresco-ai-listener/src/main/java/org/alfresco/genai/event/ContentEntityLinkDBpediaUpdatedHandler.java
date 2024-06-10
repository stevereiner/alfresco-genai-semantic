package org.alfresco.genai.event;

import org.alfresco.event.sdk.handling.filter.*;
import org.alfresco.event.sdk.handling.handler.OnNodeUpdatedEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/**
 * The {@code ContentEntityLinkDBpediaUpdatedHandler} class is a Spring component that extends the {@link AbstractContentTypeHandler}
 * and implements the {@link OnNodeUpdatedEventHandler} interface. It is responsible for handling events triggered upon the
 * update of nodes with a specified content type, focusing on nodes with the "cm:content" type and a specific DBpedia entitylinks aspect.
 *
 * <p>This handler provides a detailed event filter definition using a combination of filters to identify relevant node
 * update events. The filter criteria include the presence of the summary aspect, the "cm:content" node type, content
 * changes, or the addition of the DBpedia entitylinks aspect.
 */
@Component
public class ContentEntityLinkDBpediaUpdatedHandler extends AbstractContentTypeHandler implements OnNodeUpdatedEventHandler {

    /**
     * Aspect name associated with document summaries.
     */
    @Value("${content.service.entitylinks-dbpedia.aspect}")
    private String entityLinksDBpediaAspect;

    /**
     * Specifies the event filter to determine which node update events this handler should process. The filter criteria
     * include the presence of the DBpediaentity links aspect, the "cm:content" node type, content changes, or the addition of the
     * entitylinks aspect.
     *
     * @return An {@link EventFilter} representing the filter criteria for node update events.
     */
    @Override
    public EventFilter getEventFilter() {
        return NodeAspectFilter.of(entityLinksDBpediaAspect)
                .and(NodeTypeFilter.of("cm:content"))
                .and(ContentChangedFilter.get())
                .or(AspectAddedFilter.of(entityLinksDBpediaAspect));
    }
}