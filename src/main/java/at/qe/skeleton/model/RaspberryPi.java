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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RaspberryPi {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int violationCounter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;

    @Column(nullable = true)
    private LocalDateTime lastHeartBeat;

    @Column(nullable = false)
    @Builder.Default
    private Integer frequency = 100;

    @OneToOne(mappedBy = "raspberryPi", fetch = FetchType.LAZY)
    private RoomMonitoring roomMonitoring;
}
