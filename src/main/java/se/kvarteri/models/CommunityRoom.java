package se.kvarteri.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "communtiyrooms")
@Getter
@Setter
public class CommunityRoom {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    private String name;

    private int price;

    private int capacity;

    private LocalDateTime bookedTime;
}
