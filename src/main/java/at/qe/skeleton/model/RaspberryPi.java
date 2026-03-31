package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "raspberry_pi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaspberryPi {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int violationCounter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;

    @Column(nullable = false)
    private LocalDateTime lastHeartBeat;

    @Column(nullable = false)
    @Builder.Default
    private int frequency = 100;
}
