package dev.project.ticket.api.controller;


import dev.project.ticket.dto.TicketResponse;
import dev.project.ticket.api.service.TicketQueryService;
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


    @GetMapping("/{bookingId}")
    public ResponseEntity<List<TicketResponse>> getByBookingId(
            @PathVariable UUID bookingId) {
        var res = ticketQueryService.findByBookingId(bookingId);


        return ResponseEntity.ok(res);
    }

    @PostMapping("/{ticketId}/cancel")
    public ResponseEntity<TicketResponse> cancelTicket(
            @PathVariable UUID ticketId
    ) {
        var res = ticketQueryService.cancelTicket(ticketId);

        return ResponseEntity.ok(res);
    }


}
