package se.kvarteri.dto;

import jakarta.validation.constraints.*;
import se.kvarteri.models.*;

import java.time.LocalDateTime;
import java.util.List;
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

    public record BookingRequest(@NotNull LocalDateTime start, @NotNull LocalDateTime end) {
    }

    public record BookingResponse(UUID id, LocalDateTime start, LocalDateTime end, boolean mine) {
    }

    public record ReplyRequest(@NotBlank @Size(max = 4000) String body) {
    }

    public record ReplyResponse(UUID id, String author, String body, LocalDateTime createdAt) {
        public static ReplyResponse of(TicketReply r) {
            return new ReplyResponse(r.getId(), r.getAuthor().getName(), r.getBody(), r.getCreatedAt());
        }
    }

    public record RoomRequest(@NotBlank @Size(max = 100) String name, @Min(0) int price, @Min(1) int capacity) {
    }

    public record LaundryRequest(@NotBlank @Size(max = 100) String name) {
    }

    public record ResourceView(UUID id, String name, int price, boolean taken, boolean mine) {
    }

    public record CreateSpotRequest(@NotNull SpotKind kind, @NotBlank @Size(max = 50) String name,
                                    @Min(0) int price) {
    }

    public record PriceRequest(@Min(0) int price) {
    }

    public record AssignRequest(@NotNull UUID residentId) {
    }

    public record TenantView(UUID id, String name, String apartmentNumber) {
        public static TenantView of(Resident r) {
            return r == null ? null : new TenantView(r.getId(), r.getName(), r.getApartmentNumber());
        }
    }

    public record QueueMember(UUID id, String name) {
    }

    public record SpotView(UUID id, SpotKind kind, String name, int price, TenantView tenant,
                           List<QueueMember> queue, int queueLength, Integer myQueuePosition, boolean mine) {
    }

    public record ResidentSummary(UUID id, String name, String apartmentNumber) {
    }
}
