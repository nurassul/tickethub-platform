package dev.project.booking.utils;


import dev.project.booking.redis.service.RedisCleanupService;
import dev.project.booking.repository.postgresql.RedisCleanupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class RedisCleanupJob {

    private final RedisCleanupRepository redisCleanupRepository;
    private final RedisCleanupService redisCleanupService;


    @Scheduled(fixedDelay = 5000)
    public void cleanUp() {
        var bookingIds = redisCleanupRepository.findReadyBookingIds(Instant.now(), PageRequest.of(0,100));

        for (UUID bookingId : bookingIds) {
            try {
                redisCleanupService.process(bookingId);
            } catch (Exception e) {
                log.error("Error while cleaning: bookingId={}", bookingId, e);
            }
        }
    }

}
