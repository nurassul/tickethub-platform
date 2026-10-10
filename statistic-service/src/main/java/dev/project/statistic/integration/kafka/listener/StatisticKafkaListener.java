package dev.project.statistic.integration.kafka.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.statistic.api.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class StatisticKafkaListener {


    private final ObjectMapper objectMapper;
    private final StatisticsService statisticService;

    private static final String TICKET_GENERATED_TYPE = "ticket.generated";
    private static final String BOOKING_CONFIRMED_TYPE = "booking.confirmed";


    @KafkaListener(topics = "${app.kafka.topics.ticket-events}")
    public void listenTicketTopic(ConsumerRecord<String, String> record) throws JsonProcessingException {

        JsonNode root = objectMapper.readTree(record.value());
        String eventType = root.path("eventType").asText();

        switch (eventType) {
            case TICKET_GENERATED_TYPE -> {
                JsonNode payload = root.path("payload");
                String bookingId = payload.path("bookingId").asText();

                int ticketsCount = payload.path("ticketIds").size();

                UUID eventId = UUID.fromString(root.path("eventId").asText());

                if (statisticService.addTickets(eventId, ticketsCount)) {
                    log.info(
                            "ticket.generated handled: bookingId={}, added {} tickets",
                            bookingId, ticketsCount
                    );
                }
            }
        }


    }

    @KafkaListener(topics = "${app.kafka.topics.booking-events}")
    public void listenBookingTopic(ConsumerRecord<String, String> record) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(record.value());
        String eventType = root.path("eventType").asText();
        switch (eventType) {
            case BOOKING_CONFIRMED_TYPE -> {
                JsonNode payload = root.path("payload");
                String bookingId = payload.path("bookingId").asText();

                UUID eventId = UUID.fromString(root.path("eventId").asText());

                if (statisticService.addBooking(eventId)) {
                    log.info("booking.confirmed handled: bookingId={}", bookingId);
                }
            }
        }
    }


}
