package dev.project.event.repository.es;

import dev.project.event.elasticsearch.EventDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.UUID;

public interface EventSearchRepository extends ElasticsearchRepository<EventDocument, UUID> {
    
    List<EventDocument> findByTitleContainingIgnoreCaseOrVenueNameContainingIgnoreCase(String title, String venueName);

}
