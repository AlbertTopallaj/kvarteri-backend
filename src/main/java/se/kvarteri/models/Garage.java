package se.kvarteri.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "garages")
@Getter
@Setter
public class Garage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private int garageNumber;

    @Min(0)
    private int price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Resident owner;
}
