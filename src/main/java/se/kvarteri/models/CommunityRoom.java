package se.kvarteri.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "communtiyrooms")
@Getter
@Setter
public class CommunityRoom {

    private UUID id;
}
