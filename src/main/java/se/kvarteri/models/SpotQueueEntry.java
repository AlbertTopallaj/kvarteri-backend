package se.kvarteri.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "spot_queue_entries", uniqueConstraints =
        @UniqueConstraint(name = "uq_spot_queue_spot_resident", columnNames = {"spot_id", "resident_id"}))
@Getter
@Setter
public class SpotQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id")
    private ParkingSpot spot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id")
    private Resident resident;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
