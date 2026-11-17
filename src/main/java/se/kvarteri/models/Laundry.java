package se.kvarteri.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "laundries")
@Getter
@Setter
public class Laundry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String name;

    private LocalDateTime bookedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_by_id")
    private Resident bookedBy;
}
