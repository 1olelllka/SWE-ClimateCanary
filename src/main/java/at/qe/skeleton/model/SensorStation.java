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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SensorStation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID readId;
    @Builder.Default
    private UUID writeId = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private LocalDateTime lastHeartBeat;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_monitoring_id")
    private RoomMonitoring roomMonitoring;
}
