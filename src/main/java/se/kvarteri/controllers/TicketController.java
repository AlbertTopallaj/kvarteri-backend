package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.TicketRequest;
import se.kvarteri.dto.Dtos.TicketResponse;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Ticket;
import se.kvarteri.models.TicketStatus;
import se.kvarteri.repositories.TicketRepository;
import se.kvarteri.services.CurrentUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class TicketController {

    private final TicketRepository tickets;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TicketResponse open(@Valid @RequestBody TicketRequest req, Authentication auth) {
        Ticket t = new Ticket();
        t.setSubject(req.subject());
        t.setBody(req.body());
        t.setCreatedBy(currentUser.get(auth));
        return TicketResponse.of(tickets.save(t));
    }

    /** Residents see their own tickets, board members/admins see all. */
    @GetMapping
    List<TicketResponse> list(Authentication auth) {
        Resident me = currentUser.get(auth);
        List<Ticket> result = CurrentUser.isStaff(me)
                ? tickets.findAllByOrderByCreatedAtDesc()
                : tickets.findByCreatedByOrderByCreatedAtDesc(me);
        return result.stream().map(TicketResponse::of).toList();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('BOARD_MEMBER','ADMIN')")
    TicketResponse close(@PathVariable UUID id) {
        Ticket t = tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket not found"));
        t.setStatus(TicketStatus.CLOSED);
        return TicketResponse.of(tickets.save(t));
    }
}
