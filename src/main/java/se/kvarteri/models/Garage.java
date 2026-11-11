package se.kvarteri.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "garages")
@Getter
@Setter
public class Garage {

    private UUID id;
}
