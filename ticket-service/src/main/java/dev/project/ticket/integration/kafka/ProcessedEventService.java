package dev.project.ticket.integration.kafka;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ProcessedEventService {

    private static final String CONSUMER_NAME = "ticket-booking-events-v1";

    private final JdbcTemplate jdbcTemplate;

    public boolean tryRegister(UUID eventId) {
        String query = "INSERT INTO processed_events (consumer_name, event_id) " +
                "VALUES (?, ?) " +
                "ON CONFLICT (consumer_name, event_id) DO NOTHING";

        int res = jdbcTemplate.update(query, CONSUMER_NAME, eventId);

        return res == 1;
    }

}
