package dev.project.booking.redis.service;


import dev.project.booking.repository.entity.RedisTask;
import dev.project.booking.repository.entity.enums.RedisTaskOperation;
import dev.project.booking.repository.postgresql.RedisCleanupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisTaskService {

    private final RedisCleanupRepository redisCleanupRepository;


    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(UUID bookingId, RedisTaskOperation operation) {
        var now = Instant.now();
        redisCleanupRepository.insertIfAbsent(UUID.randomUUID(), bookingId, operation.name(), now);
    }

}
