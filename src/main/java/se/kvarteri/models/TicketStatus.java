package se.kvarteri.models;

public enum TicketStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    REJECTED;

    public boolean isClosed() {
        return this == DONE || this == REJECTED;
    }
}
