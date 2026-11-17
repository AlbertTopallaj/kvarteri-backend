package se.kvarteri.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** A parking spot or garage rented indefinitely by one tenant, with a queue of interested residents. */
@Entity
@Table(name = "parking_spots", uniqueConstraints =
        @UniqueConstraint(name = "uq_parking_spots_kind_name", columnNames = {"kind", "name"}))
@Getter
@Setter
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotKind kind;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Min(0)
    private int price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Resident tenant;
}
