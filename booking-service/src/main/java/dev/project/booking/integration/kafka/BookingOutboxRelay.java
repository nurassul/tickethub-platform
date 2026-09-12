package dev.project.booking.integration.kafka;


import dev.project.booking.repository.entity.OutboxEvent;
import dev.project.booking.repository.postgresql.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingOutboxRelay {


    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.outbox.relay.batch-size}")
    private int batchSize;


    @Scheduled(
            fixedDelayString = "${app.outbox.relay.fixed-delay-ms}"
    )
    public void publishPendingEvent() {
        List<OutboxEvent> events =
                outboxEventRepository
                        .findByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                                Instant.now(),
                                PageRequest.of(0, batchSize)
                        );
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(
                        event.getTopic(),
                        event.getMessageKey(),
                        event.getPayload()
                ).get();

                event.setPublishedAt(Instant.now());
                event.setLastError(null);
                outboxEventRepository.save(event);

                log.info(
                        "Kafka event published: eventId={}, type={}. topic={}, key={}",
                        event.getId(),
                        event.getEventType(),
                        event.getTopic(),
                        event.getMessageKey()
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                markAttemptFailed(event, e);
                return;
            } catch (Exception e) {
                markAttemptFailed(event, e);
                return;
            }
        }
    }

    private void markAttemptFailed(
            OutboxEvent event,
            Exception e
    ) {
        int attempts = event.getAttempts() + 1;

        long retryDelaySeconds = Math.min(
                300L,
                1L << Math.min(attempts, 9)
        );

        event.setAttempts(attempts);
        event.setLastError(e.toString());
        event.setNextAttemptAt(
                Instant.now().plusSeconds(retryDelaySeconds)
        );

        outboxEventRepository.save(event);

        log.warn(
                "Kafka event publish failed: eventId={}, type={}, attempt={}, retryAfterSeconds={}, error={}",
                event.getId(),
                event.getEventType(),
                attempts,
                retryDelaySeconds,
                e.getClass().getSimpleName()
        );
    }

}
