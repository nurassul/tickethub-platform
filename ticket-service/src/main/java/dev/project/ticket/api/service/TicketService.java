package dev.project.ticket.api.service;


import dev.project.ticket.dto.TicketScanResponse;
import dev.project.ticket.repository.entity.enums.TicketStatus;
import dev.project.ticket.repository.postgresql.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TicketService {

    private final TicketRepository ticketRepository;


    @Transactional
    public TicketScanResponse scanTicket(UUID ticketId) {
        var ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found id: " + ticketId));


        switch (ticket.getStatus()) {
            case ACTIVE -> {
                ticket.setStatus(TicketStatus.SCANNED);

                return new TicketScanResponse(true, "Success! Ticket is ACTIVE.");
            }
            case CANCELLED -> {
                return new TicketScanResponse(false, "Fail! Ticket is CANCELLED.");
            }
            case SCANNED -> {
                return new TicketScanResponse(false, "Fail! Ticket was SCANNED.");
            }
            default -> {
                return new TicketScanResponse(false, "Fail! Invalid TicketStatus");
            }
        }

    }

}
