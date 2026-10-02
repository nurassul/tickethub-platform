package dev.project.ticket.api.controller;


import dev.project.ticket.api.service.TicketService;
import dev.project.ticket.dto.TicketResponse;
import dev.project.ticket.api.service.TicketQueryService;
import dev.project.ticket.dto.TicketScanResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {


    private final TicketQueryService ticketQueryService;
    private final TicketService ticketService;


    @GetMapping("/{bookingId}")
    public ResponseEntity<List<TicketResponse>> getByBookingId(
            @PathVariable UUID bookingId,
            @RequestHeader(value = "X-Booking-Token", required = false)
            String guestToken
    ) {
        var res = ticketQueryService.findByBookingId(bookingId, guestToken);


        return ResponseEntity.ok(res);
    }

    @PostMapping("/{ticketId}/cancel")
    public ResponseEntity<TicketResponse> cancelTicket(
            @PathVariable UUID ticketId,
            @RequestHeader(value = "X-Booking-Token", required = false)
            String guestToken
    ) {
        var res = ticketQueryService.cancelTicket(ticketId, guestToken);

        return ResponseEntity.ok(res);
    }

    @PostMapping("/{ticketId}/scan")
    public ResponseEntity<TicketScanResponse> scanTicket(
            @PathVariable UUID ticketId
    ) {

        var res = ticketService.scanTicket(ticketId);

        return ResponseEntity.ok(res);
    }


}
