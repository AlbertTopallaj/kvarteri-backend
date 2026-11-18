package se.kvarteri.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** A formal warning given to a resident by a board member or admin. */
@Entity
@Table(name = "warnings")
@Getter
@Setter
public class Warning {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Resident resident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Resident issuedBy;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
