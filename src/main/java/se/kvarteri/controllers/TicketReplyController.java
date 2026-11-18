package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.ReplyRequest;
import se.kvarteri.dto.Dtos.ReplyResponse;
import se.kvarteri.exception.ConflictException;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Ticket;
import se.kvarteri.models.TicketReply;
import se.kvarteri.models.TicketStatus;
import se.kvarteri.repositories.TicketReplyRepository;
import se.kvarteri.repositories.TicketRepository;
import se.kvarteri.services.CurrentUser;

import java.util.List;
import java.util.UUID;

/** Conversation thread on a ticket: only the ticket's creator and staff may read or write. */
@RestController
@RequestMapping("/api/tickets/{ticketId}/replies")
@RequiredArgsConstructor
public class TicketReplyController {

    private final TicketRepository tickets;
    private final TicketReplyRepository replies;
    private final CurrentUser currentUser;

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    List<ReplyResponse> list(@PathVariable UUID ticketId, Authentication auth) {
        Ticket t = accessible(ticketId, currentUser.get(auth));
        return replies.findByTicketOrderByCreatedAt(t).stream().map(ReplyResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.transaction.annotation.Transactional
    ReplyResponse reply(@PathVariable UUID ticketId, @Valid @RequestBody ReplyRequest req, Authentication auth) {
        Resident me = currentUser.get(auth);
        Ticket t = accessible(ticketId, me);
        if (t.getStatus().isClosed()) throw new ConflictException("Ticket is closed");
        TicketReply r = new TicketReply();
        r.setTicket(t);
        r.setAuthor(me);
        r.setBody(req.body());
        return ReplyResponse.of(replies.save(r));
    }

    private Ticket accessible(UUID id, Resident me) {
        Ticket t = tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket not found"));
        if (!CurrentUser.sameAssociation(t.getCreatedBy(), me)
                || (!CurrentUser.isStaff(me) && !t.getCreatedBy().getId().equals(me.getId()))) {
            // Same response as a missing ticket so ids cannot be probed.
            throw new NotFoundException("Ticket not found");
        }
        return t;
    }
}
