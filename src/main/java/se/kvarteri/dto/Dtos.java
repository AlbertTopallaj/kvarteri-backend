package se.kvarteri.dto;

import jakarta.validation.constraints.*;
import se.kvarteri.models.*;

import java.time.LocalDateTime;
import java.util.UUID;

public final class Dtos {

    private Dtos() {
    }

    public record CreateResidentRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 10, max = 72) String password,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 20) String apartmentNumber,
            @NotNull Role role) {
    }

    public record ResidentResponse(UUID id, String email, String name, String apartmentNumber, Role role) {
        public static ResidentResponse of(Resident r) {
            return new ResidentResponse(r.getId(), r.getEmail(), r.getName(), r.getApartmentNumber(), r.getRole());
        }
    }

    public record MessageRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 4000) String body,
            UUID recipientId) {
    }

    public record MessageResponse(UUID id, String title, String body, String author, boolean personal,
                                  LocalDateTime createdAt) {
        public static MessageResponse of(Message m) {
            return new MessageResponse(m.getId(), m.getTitle(), m.getBody(), m.getAuthor().getName(),
                    m.getRecipient() != null, m.getCreatedAt());
        }
    }

    public record TicketRequest(
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 4000) String body) {
    }

    public record TicketResponse(UUID id, String subject, String body, String createdBy, TicketStatus status,
                                 LocalDateTime createdAt) {
        public static TicketResponse of(Ticket t) {
            return new TicketResponse(t.getId(), t.getSubject(), t.getBody(), t.getCreatedBy().getName(),
                    t.getStatus(), t.getCreatedAt());
        }
    }

    public record BookingRequest(@NotNull LocalDateTime time) {
    }
}
