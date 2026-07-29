package dev.project.event.api.service.impl;


import dev.project.event.api.service.SeatService;
import dev.project.event.dto.seat.CreateSeatBatchRequest;
import dev.project.event.dto.seat.SeatBatchResponse;
import dev.project.event.dto.seat.SeatResponse;
import dev.project.event.dto.seat.booking.ValidateSeatsRequest;
import dev.project.event.dto.seat.booking.ValidatedSeatResponse;
import dev.project.event.dto.seat.booking.ValidatedSeatsResponse;
import dev.project.event.repository.EventRepository;
import dev.project.event.repository.SeatRepository;
import dev.project.event.repository.entity.Event;
import dev.project.event.repository.entity.Seat;
import dev.project.event.repository.entity.enums.EventStatus;
import dev.project.event.utils.SeatMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Transactional
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final SeatMapper mapper;

    @Override
    public SeatBatchResponse createSeats(UUID eventID, CreateSeatBatchRequest request) {
        Event event = eventRepository.findById(eventID)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id=" + eventID));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalStateException(
                    "Seats can be added only to a draft event"
            );
        }

        if (request.seats() == null || request.seats().isEmpty()) {
            throw new IllegalArgumentException(
                    "Seats list must not be empty"
            );
        }

        List<Seat> seatsToSave = request.seats().stream()
                .map(mapper::toEntity)
                .peek(seat -> seat.setEvent(event))
                .toList();

        List<SeatResponse> seats = seatRepository.saveAll(seatsToSave).stream()
                .map(mapper::toResponse)
                .toList();

        return new SeatBatchResponse(eventID, seats);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatBatchResponse getSeatsByEventId(UUID eventID) {
        Event event = eventRepository.findById(eventID)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id=" + eventID));

        List<SeatResponse> seats = seatRepository.findAllByEvent(event).stream()
                .map(mapper::toResponse)
                .toList();

        return new SeatBatchResponse(eventID, seats);
    }

    @Transactional(readOnly = true)
    @Override
    public ValidatedSeatsResponse validateSeats(
            UUID eventId,
            ValidateSeatsRequest request
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id=" + eventId));


        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Bookings are allowed only for PUBLISHED events!"
            );
        }

        Set<UUID> uniqueSeatIds = new HashSet<>(request.seatIds());

        if (uniqueSeatIds.size() != request.seatIds().size()){
            throw new IllegalArgumentException("Seat IDs must be unique");
        }

        List<Seat> seats = seatRepository.findAllByEvent_IdAndIdIn(
                eventId,
                uniqueSeatIds
        );
        if (seats.size() != uniqueSeatIds.size()) {
            throw new IllegalArgumentException(
                    "Some seats do not exist or do not belong to this event"
            );
        }

        List<ValidatedSeatResponse> responseSeats = seats.stream()
                .map(seat -> new ValidatedSeatResponse(
                        seat.getId(),
                        seat.getPrice()
                ))
                .toList();

        return new ValidatedSeatsResponse(eventId, responseSeats);
    }
}
