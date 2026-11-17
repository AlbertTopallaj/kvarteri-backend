package se.kvarteri.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Message from a board member. recipient == null means it goes to the whole complex. */
@Entity
@Table(name = "messages")
@Getter
@Setter
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String body;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Resident author;

    @ManyToOne(fetch = FetchType.LAZY)
    private Resident recipient;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
