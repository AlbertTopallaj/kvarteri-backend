package se.kvarteri.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** A housing association (förening). Residents, resources, tickets and messages are all scoped to one. */
@Entity
@Table(name = "associations")
@Getter
@Setter
public class Association {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;
}
