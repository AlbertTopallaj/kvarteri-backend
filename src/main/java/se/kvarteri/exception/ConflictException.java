package se.kvarteri.exception;

/** Thrown when a request conflicts with current state, e.g. a resource that is already booked. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
