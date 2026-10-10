package dev.project.booking.redis.service;


import dev.project.booking.repository.entity.BookingSeat;
import dev.project.booking.repository.entity.enums.BookingSeatStatus;
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
    private final BookingRedisSyncService bookingRedisSyncService;


    @Transactional
    public void process(UUID taskId) {
        var task = redisCleanupRepository.findByIdForUpdate(taskId);
        if (task.isEmpty()) {
            return;
        }

        var redisCleanupTask = task.get();
        var bookingId = redisCleanupTask.getBookingId();

        if (redisCleanupTask.getNextAttemptAt().isAfter(Instant.now())) {
            return;
        }


        try {
            switch (redisCleanupTask.getOperation()) {
                case RELEASE -> {
                    releaseSeats(bookingId);
                }

                case MARK_SOLD -> {
                    bookingRedisSyncService.markSoldIfActive(bookingId);
                }
            }

        } catch (DataAccessException e) {
            redisCleanupTask.setAttempts(redisCleanupTask.getAttempts() + 1);
            redisCleanupTask.setNextAttemptAt(Instant.now().plusSeconds(30));

            log.warn("Error while doing task in redis: taskId={}, operation={}, bookingId={}, attempts={}", taskId, redisCleanupTask.getOperation(), bookingId, redisCleanupTask.getAttempts(), e);
            return;
        }

        redisCleanupRepository.delete(redisCleanupTask);
    }


    private void releaseSeats(UUID bookingId) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found id= " + bookingId));

        var seats = bookingSeatRepository.findAllByBooking_Id(bookingId);

        List<UUID> seatsIds = seats.stream()
                .filter(seat -> switch (booking.getStatus()) {
                        case CANCELLED, EXPIRED, PAYMENT_FAILED -> true;

                        case CONFIRMED, PARTIALLY_CANCELLED ->
                            seat.getStatus() == BookingSeatStatus.CANCELLED;

                        case HOLD -> false;
                })
                .map(BookingSeat::getSeatId)
                .toList();

        if (!seatsIds.isEmpty()) {
            seatHoldService.release(bookingId, booking.getEventId(), seatsIds);
            seatHoldService.releaseSold(bookingId, booking.getEventId(), seatsIds);
        }
    }

}
