package dev.project.ticket.api.service;


import dev.project.ticket.dto.TicketResponse;
import dev.project.ticket.integration.TicketOutboxService;
import dev.project.ticket.repository.entity.enums.TicketStatus;
import dev.project.ticket.repository.postgresql.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@Service
public class TicketQueryService {


    private final TicketRepository ticketRepository;
    private final TicketOutboxService ticketOutboxService;


    @Transactional
    public TicketResponse cancelTicket(UUID ticketId) {
        var ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ticket not found: " + ticketId
                ));

        if (ticket.getStatus() != TicketStatus.CANCELLED) {
            ticket.setStatus(TicketStatus.CANCELLED);

            ticketOutboxService.saveTicketCancelled(ticket);
        }




        return new TicketResponse(
                ticket.getId(),
                ticket.getBookingId(),
                ticket.getEventId(),
                ticket.getSeatId(),
                ticket.getStatus(),
                ticket.getCreatedAt()
        );

    }


    @Transactional(readOnly = true)
    public List<TicketResponse> findByBookingId(UUID bookingId) {
        List<TicketResponse> result = new ArrayList<>();

        var tickets = ticketRepository.findAllByBookingId(bookingId);

        for (var ticket : tickets) {
            result.add(
                    new TicketResponse(
                            ticket.getId(),
                            ticket.getBookingId(),
                            ticket.getEventId(),
                            ticket.getSeatId(),
                            ticket.getStatus(),
                            ticket.getCreatedAt()
                    )
            );
        }

        return result;

    }


}
