package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.MessageRequest;
import se.kvarteri.dto.Dtos.MessageResponse;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.Message;
import se.kvarteri.models.Resident;
import se.kvarteri.repositories.MessageRepository;
import se.kvarteri.repositories.ResidentRepository;
import se.kvarteri.services.CurrentUser;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class MessageController {

    private final MessageRepository messages;
    private final ResidentRepository residents;
    private final CurrentUser currentUser;

    @GetMapping
    List<MessageResponse> mine(Authentication auth) {
        return messages.findVisibleTo(currentUser.get(auth)).stream().map(MessageResponse::of).toList();
    }

    /** Board members/admins only (enforced in SecurityConfig). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MessageResponse send(@Valid @RequestBody MessageRequest req, Authentication auth) {
        Message m = new Message();
        m.setTitle(req.title());
        m.setBody(req.body());
        m.setAuthor(currentUser.get(auth));
        if (req.recipientId() != null) {
            Resident rec = residents.findById(req.recipientId())
                    .orElseThrow(() -> new NotFoundException("Recipient not found"));
            m.setRecipient(rec);
        }
        return MessageResponse.of(messages.save(m));
    }
}
