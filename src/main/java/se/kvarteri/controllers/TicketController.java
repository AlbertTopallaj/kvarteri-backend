package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.TicketRequest;
import se.kvarteri.dto.Dtos.TicketResponse;
import se.kvarteri.dto.Dtos.TicketStatusRequest;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.*;
import se.kvarteri.repositories.TicketRepository;
import se.kvarteri.services.CurrentUser;
import se.kvarteri.services.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Transactional
public class TicketController {

    private static final Map<TicketStatus, String> STATUS_LABELS = Map.of(
            TicketStatus.PENDING, "Väntar",
            TicketStatus.IN_PROGRESS, "Pågår",
            TicketStatus.DONE, "Klar",
            TicketStatus.REJECTED, "Avslaget");

    private final TicketRepository tickets;
    private final CurrentUser currentUser;
    private final NotificationService notifications;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TicketResponse open(@Valid @RequestBody TicketRequest req, Authentication auth) {
        Resident me = currentUser.get(auth);
        Ticket t = new Ticket();
        t.setSubject(req.subject().trim());
        t.setBody(req.body() == null ? "" : req.body().trim());
        t.setKind(req.kind() == null ? TicketKind.ERRAND : req.kind());
        t.setCreatedBy(me);
        t = tickets.save(t);
        String label = t.getKind() == TicketKind.INCIDENT ? "felanmälan" : "ärende";
        notifications.notifyRole(me.getAssociation(), Role.BOARD_MEMBER, "Ny " + label + " från " + me.getName() + ": " + t.getSubject());
        return TicketResponse.of(t);
    }

    /** Residents see their own tickets, board members/admins see all. */
    @GetMapping
    @Transactional(readOnly = true)
    List<TicketResponse> list(Authentication auth) {
        Resident me = currentUser.get(auth);
        List<Ticket> result = CurrentUser.isStaff(me)
                ? tickets.findByCreatedByAssociationOrderByCreatedAtDesc(me.getAssociation())
                : tickets.findByCreatedByOrderByCreatedAtDesc(me);
        return result.stream().map(TicketResponse::of).toList();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('BOARD_MEMBER','ADMIN')")
    TicketResponse setStatus(@PathVariable UUID id, @Valid @RequestBody TicketStatusRequest req, Authentication auth) {
        Ticket t = tickets.findById(id)
                .filter(x -> CurrentUser.sameAssociation(x.getCreatedBy(), currentUser.get(auth)))
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
        if (t.getStatus() != req.status()) {
            t.setStatus(req.status());
            notifications.notify(t.getCreatedBy(), "Ditt ärende \"" + t.getSubject() + "\" har status: "
                    + STATUS_LABELS.get(req.status()));
        }
        return TicketResponse.of(t);
    }
}
