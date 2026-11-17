package se.kvarteri.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** A user of the system (resident, board member or admin, separated by {@link Role}). */
@Entity
@Table(name = "residents")
@Getter
@Setter
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash, never the plain password. */
    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String apartmentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.RESIDENT;

    @Column(nullable = false)
    private boolean enabled = true;
}
