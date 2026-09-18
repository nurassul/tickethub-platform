package dev.project.event.kafka.listener;


import dev.project.event.elasticsearch.EventDocument;
import dev.project.event.kafka.event.EventSyncMessage;
import dev.project.event.repository.es.EventSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Slf4j
@RequiredArgsConstructor
@Component
public class EventSearchSyncListener {

    private final EventSearchRepository eventSearchRepository;


    @KafkaListener(topics = "${KAFKA_EVENTS_TOPIC:tickethub.events.changes.v1}")
    public void listen(EventSyncMessage message) {

        var event = EventDocument.builder()
                .id(message.id())
                .title(message.title())
                .description(message.description())
                .city(message.city())
                .venueName(message.venueName())
                .status(message.status())
                .startsAt(message.startsAt())
                .build();

        eventSearchRepository.save(event);

        log.info(
                "Event document was handled: id={}, title={}",
                message.id(),
                message.title()
        );
    }
}
