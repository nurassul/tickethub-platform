package dev.project.event.utils;

import dev.project.event.dto.seat.CreateSeatRequest;
import dev.project.event.dto.seat.SeatResponse;
import dev.project.event.repository.entity.Seat;
import org.springframework.stereotype.Component;


@Component
public class SeatMapper {

    public Seat toEntity(CreateSeatRequest request) {
        return Seat.builder()
                .sector(request.sector())
                .rowNumber(request.rowNumber())
                .seatNumber(request.seatNumber())
                .type(request.type())
                .price(request.price())
                .build();
    }


    public SeatResponse toResponse(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getSector(),
                seat.getRowNumber(),
                seat.getSeatNumber(),
                seat.getType(),
                seat.getPrice()
        );
    }

}
