package dev.project.ticket.integration.kafka;


import dev.project.ticket.integration.kafka.event.BookingConfirmedEvent;
import dev.project.ticket.repository.entity.Ticket;
import dev.project.ticket.repository.postgresql.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class BookingConfirmedHandler {

    private final ProcessedEventService processedEventService;
    private final TicketRepository ticketRepository;


    @Transactional
    public void handle(BookingConfirmedEvent event) {
        if (processedEventService.tryRegister(event.eventId())) {
            for (UUID seatId : event.payload().seatIds()) {
                var ticket = Ticket.builder()
                        .bookingId(event.payload().bookingId())
                        .eventId(event.payload().eventId())
                        .seatId(seatId)
                        .build();
                ticketRepository.save(ticket);
            }
        }
    }

}
