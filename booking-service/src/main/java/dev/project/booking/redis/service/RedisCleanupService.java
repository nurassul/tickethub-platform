package dev.project.booking.redis.service;


import dev.project.booking.repository.entity.BookingSeat;
import dev.project.booking.repository.postgresql.BookingRepository;
import dev.project.booking.repository.postgresql.BookingSeatRepository;
import dev.project.booking.repository.postgresql.RedisCleanupRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCleanupService {

    private final RedisCleanupRepository redisCleanupRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatHoldService seatHoldService;


    @Transactional
    public void process(UUID bookingId) {
        var task = redisCleanupRepository.findByBookingIdForUpdate(bookingId);
        if (task.isEmpty()) {
            return;
        }

        var redisCleanupTask = task.get();

        if (redisCleanupTask.getNextAttemptAt().isAfter(Instant.now())) {
            return;
        }

        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found id= " + bookingId ));

        var seats = bookingSeatRepository.findAllByBooking_Id(bookingId);

        List<UUID> seatsIds = seats.stream()
                .map(BookingSeat::getSeatId)
                .toList();

        try {
            if (!seatsIds.isEmpty()) {
                seatHoldService.release(bookingId, booking.getEventId(), seatsIds);
                seatHoldService.releaseSold(bookingId, booking.getEventId(), seatsIds);
            }
        } catch (DataAccessException e) {
            redisCleanupTask.setAttempts(redisCleanupTask.getAttempts() + 1);
            redisCleanupTask.setNextAttemptAt(Instant.now().plusSeconds(30));

            log.warn("Error while cleaning in redis: bookingId={}, attempts={}", bookingId, redisCleanupTask.getAttempts(), e);
            return;
        }

        redisCleanupRepository.delete(redisCleanupTask);
    }

}
