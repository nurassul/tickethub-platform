package dev.project.booking.integration.kafka.service;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final JdbcTemplate jdbcTemplate;


    public boolean tryRegister(String consumerName, UUID eventId) {
        String query = "INSERT INTO processed_events (consumer_name, event_id) VALUES (?, ?) " +
                "ON CONFLICT (consumer_name, event_id) DO NOTHING";

        int res = jdbcTemplate.update(query, consumerName, eventId);

        return res == 1;
    }

}
