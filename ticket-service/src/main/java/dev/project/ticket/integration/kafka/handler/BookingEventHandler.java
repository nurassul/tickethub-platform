package dev.project.ticket.integration.kafka.handler;


import dev.project.ticket.integration.TicketOutboxService;
import dev.project.ticket.integration.kafka.event.BookingCancelledEvent;
import dev.project.ticket.integration.kafka.service.BookingLockService;
import dev.project.ticket.integration.kafka.service.ProcessedEventService;
import dev.project.ticket.integration.kafka.event.BookingConfirmedEvent;
import dev.project.ticket.repository.entity.CancelledBooking;
import dev.project.ticket.repository.entity.Ticket;
import dev.project.ticket.repository.entity.enums.TicketStatus;
import dev.project.ticket.repository.postgresql.CancelledBookingsRepository;
import dev.project.ticket.repository.postgresql.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class BookingEventHandler {

    private final ProcessedEventService processedEventService;
    private final TicketRepository ticketRepository;
    private final TicketOutboxService ticketOutboxService;
    private final BookingLockService bookingLockService;
    private final CancelledBookingsRepository cancelledBookingsRepository;


    @Transactional
    public void handleConfirmed(BookingConfirmedEvent event) {
        if (processedEventService.tryRegister(event.eventId())) {

            bookingLockService.lock(event.payload().bookingId());
            if (cancelledBookingsRepository.existsById(event.payload().bookingId())) {
                log.debug("Booking already cancelled!");
                return;
            }

            List<UUID> ticketIds = new ArrayList<>();

            for (UUID seatId : event.payload().seatIds()) {
                var ticket = Ticket.builder()
                        .bookingId(event.payload().bookingId())
                        .eventId(event.payload().eventId())
                        .seatId(seatId)
                        .build();
                ticket = ticketRepository.save(ticket);
                ticketIds.add(ticket.getId());
            }

            ticketOutboxService.saveTicketsGenerated(
                    event.payload().bookingId(),
                    event.payload().eventId(),
                    event.payload().customerEmail(),
                    ticketIds
            );

            log.info("Tickets created: bookingId={}, ticketIds={}",
                    event.payload().bookingId(), ticketIds);
        }
    }

    @Transactional
    public void handleCancelled(BookingCancelledEvent event) {
        if (processedEventService.tryRegister(event.eventId())) {
            bookingLockService.lock(event.payload().bookingId());

            CancelledBooking cancelledBooking = CancelledBooking.builder()
                    .bookingId(event.payload().bookingId())
                    .cancelledAt(event.payload().cancelledAt())
                    .build();

            cancelledBookingsRepository.save(cancelledBooking);

            var tickets = ticketRepository.findAllByBookingIdForUpdate(event.payload().bookingId());
            tickets.forEach(
                    e -> {
                        if (e.getStatus() == TicketStatus.ACTIVE) {
                            e.setStatus(TicketStatus.CANCELLED);
                        }
                    }
            );

            log.info("Tickets cancelled: bookingId={}, messageId={}",
                    event.payload().bookingId(), event.eventId());
        }
    }

}
