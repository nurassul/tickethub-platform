package dev.project.booking.integration.kafka;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private static final String CONSUMER_NAME = "booking-payment-events-v1";

    private final JdbcTemplate jdbcTemplate;


    public boolean tryRegister(UUID eventId) {
        String query = "INSERT INTO processed_events (consumer_name, event_id) VALUES (?, ?) " +
                "ON CONFLICT (consumer_name, event_id) DO NOTHING";

        int res = jdbcTemplate.update(query, CONSUMER_NAME, eventId);

        return res == 1;
    }

}
