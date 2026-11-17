package se.kvarteri.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.NotificationResponse;
import se.kvarteri.models.Resident;
import se.kvarteri.repositories.NotificationRepository;
import se.kvarteri.services.CurrentUser;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Transactional
public class NotificationController {

    private final NotificationRepository notifications;
    private final CurrentUser currentUser;

    /** The 50 latest notifications for the logged in user, newest first. */
    @GetMapping
    @Transactional(readOnly = true)
    List<NotificationResponse> mine(Authentication auth) {
        Resident me = currentUser.get(auth);
        return notifications.findTop50ByRecipientOrderByCreatedAtDesc(me).stream()
                .map(n -> new NotificationResponse(n.getId(), n.getText(), n.getCreatedAt(), n.isSeen())).toList();
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void readAll(Authentication auth) {
        notifications.markAllSeen(currentUser.get(auth));
    }
}
