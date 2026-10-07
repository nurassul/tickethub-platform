package dev.project.ticket.integration.kafka.service;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class BookingLockService {
    private final JdbcTemplate jdbcTemplate;


    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(UUID bookingId) {
        String query = "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))";
        String lockKey = "booking:" + bookingId.toString();

        ResultSetExtractor<Void> extractor = rs -> null;

        jdbcTemplate.query(query, extractor, lockKey);
    }
}
