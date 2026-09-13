package dev.project.ticket.integration.kafka;


import dev.project.ticket.repository.postgresql.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketOutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;


    @Scheduled(fixedDelayString = "${app.kafka.outbox.relay-delay-ms:1000}")
    public void publishPendingEvent(){
        var events = outboxEventRepository
                .findTop100ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        Instant.now()
                );

        for (var event : events) {
            try {
                kafkaTemplate.send(
                        event.getTopic(),
                        event.getMessageKey(),
                        event.getPayload()
                ).get();

                outboxEventRepository.markPublished(event.getId(), Instant.now());

                log.info(
                        "Kafka event published: eventId={}, type={}. topic={}, key={}",
                        event.getId(),
                        event.getEventType(),
                        event.getTopic(),
                        event.getMessageKey()
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                outboxEventRepository.markFailed(
                        event.getId(),
                        e.getMessage(),
                        Instant.now().plusSeconds(5)
                );

                log.warn(
                        "Kafka event publishing interrupted: eventId={}, type={}, error={}",
                        event.getId(),
                        event.getEventType(),
                        e.getMessage()
                );
            }
            catch (Exception e) {
                outboxEventRepository.markFailed(
                        event.getId(),
                        e.getMessage(),
                        Instant.now().plusSeconds(5)
                );

                log.warn(
                        "Kafka event not published: eventId={}, type={}, topic={}, key={}, error={}",
                        event.getId(),
                        event.getEventType(),
                        event.getTopic(),
                        event.getMessageKey(),
                        e.getMessage()
                );
            }

        }
    }

}
