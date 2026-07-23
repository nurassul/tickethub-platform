package dev.project.event.api.service;

import dev.project.event.dto.seat.CreateSeatBatchRequest;
import dev.project.event.dto.seat.SeatBatchResponse;
import dev.project.event.dto.seat.SeatResponse;

import java.util.UUID;

public interface SeatService {

    SeatBatchResponse createSeats(UUID eventID, CreateSeatBatchRequest request);

    SeatBatchResponse getSeatsByEventId(UUID eventID);
}
