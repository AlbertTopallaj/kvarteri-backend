package se.kvarteri.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "parkings")
@Getter
@Setter
public class Parking {

    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
}
