package se.kvarteri.models;

import jakarta.persistence.*;
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

    private int garageNumber;

    private int price;


}
