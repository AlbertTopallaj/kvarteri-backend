package se.kvarteri.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "parkings")
@Getter
@Setter
public class Parking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private int parkingNumber;

    @Min(0)
    private int price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Resident tenant;
}
