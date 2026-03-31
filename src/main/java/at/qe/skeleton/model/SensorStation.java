package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sensor_station")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorStation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime lastHeartBeat;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceStatus status;

}
