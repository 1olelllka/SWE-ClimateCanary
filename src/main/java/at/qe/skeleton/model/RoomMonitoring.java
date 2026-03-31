package at.qe.skeleton.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class RoomMonitoring {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID roomId;
}
