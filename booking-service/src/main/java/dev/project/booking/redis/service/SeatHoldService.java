package dev.project.booking.redis.service;


import dev.project.booking.api.exceptions.BusinessConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private static final DefaultRedisScript<Long> HOLD_SCRIPT =
            new DefaultRedisScript<>("""
                for i, key in ipairs(KEYS) do
                    if redis.call('exists', key) == 1 then
                        return 0
                    end
                end

                for i, key in ipairs(KEYS) do
                    redis.call('set', key, ARGV[1], 'PXAT', ARGV[2])
                end

                return 1
                """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>("""
                local released = 0

                for i, key in ipairs(KEYS) do
                    if redis.call('get', key) == ARGV[1] then
                        redis.call('del', key)
                        released = released + 1
                    end
                end

                return released
                """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_SOLD_SCRIPT =
            new DefaultRedisScript<>("""
                local released = 0

                for i, key in ipairs(KEYS) do
                    if redis.call('get', key) == 'SOLD:' .. ARGV[1] then
                        redis.call('del', key)
                        released = released + 1
                    end
                end

                return released
                """, Long.class);

    private static final DefaultRedisScript<Long> MARK_SOLD_SCRIPT =
            new DefaultRedisScript<>("""
            for i, key in ipairs(KEYS) do
                redis.call('set', key, 'SOLD:' .. ARGV[1])
            end

            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public boolean tryHold(
            UUID bookingId,
            UUID eventId,
            List<UUID> seatIds,
            LocalDateTime expiresAt
    ) {
        long expiresAtMilli = expiresAt.toInstant(ZoneOffset.UTC).toEpochMilli();
        if (!expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessConflictException("Booking has expired");
        }

        List<String> keys = buildKeys(eventId, seatIds);

        Long result = redisTemplate.execute(
                HOLD_SCRIPT,
                keys,
                bookingId.toString(),
                String.valueOf(expiresAtMilli)
        );

        return Long.valueOf(1).equals(result);
    }

    public void release(
            UUID bookingId,
            UUID eventId,
            List<UUID> seatIds
    ) {
        redisTemplate.execute(
                RELEASE_SCRIPT,
                buildKeys(eventId, seatIds),
                bookingId.toString()
        );
    }

    public void markSold(
            UUID bookingId,
            UUID eventId,
            List<UUID> seatIds
    ) {
        redisTemplate.execute(
                MARK_SOLD_SCRIPT,
                buildKeys(eventId, seatIds),
                bookingId.toString()
        );
    }

    public void releaseSold(
            UUID bookingId,
            UUID eventId,
            List<UUID> seatIds
    ) {
        redisTemplate.execute(
                RELEASE_SOLD_SCRIPT,
                buildKeys(eventId,seatIds),
                bookingId.toString()
        );
    }

    private List<String> buildKeys(UUID eventId, List<UUID> seatIds) {
        return seatIds.stream()
                .map(seatId -> "seat:{" + eventId + "}:" + seatId)
                .toList();
    }



}
