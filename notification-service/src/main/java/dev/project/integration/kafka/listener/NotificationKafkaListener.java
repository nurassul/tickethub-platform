package dev.project.integration.kafka.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.integration.kafka.handler.TicketEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaListener {

    private final ObjectMapper objectMapper;
    private final TicketEventHandler ticketEventHandler;

    private static final String TICKET_GENERATED_TYPE = "ticket.generated";

    @KafkaListener(topics = "${app.kafka.topics.ticket-events}")
    public void listen(ConsumerRecord<String, String> record) throws JsonProcessingException {

        JsonNode root = objectMapper.readTree(record.value());
        String eventType = root.path("eventType").asText();

        switch (eventType) {
            case TICKET_GENERATED_TYPE -> {
                JsonNode payload = root.path("payload");

                String customerEmail = payload.path("customerEmail").asText();
                String bookingId = payload.path("bookingId").asText();

                List<String> ticketIds = new ArrayList<>();
                payload.path("ticketIds")
                        .forEach(node -> ticketIds.add(node.asText()));

                ticketEventHandler.handleTicketsGenerated(
                        customerEmail,
                        bookingId,
                        ticketIds
                );

                log.info("ticket.generated handled: bookingId={}", bookingId);
            }
        }


    }

}
