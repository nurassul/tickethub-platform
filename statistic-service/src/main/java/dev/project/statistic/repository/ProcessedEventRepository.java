package dev.project.statistic.repository;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProcessedEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean tryRegister(String consumerName, UUID eventId) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO processed_events (consumer_name, event_id)
                VALUES (?, ?)
                ON CONFLICT (consumer_name, event_id) DO NOTHING
                """, consumerName, eventId);

        return inserted == 1;
    }

}
