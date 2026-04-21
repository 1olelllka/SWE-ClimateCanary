package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
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

    @Column(nullable = false)
    private Integer port;

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

    @OneToMany(mappedBy = "raspberryPi", fetch = FetchType.LAZY)
    private Set<RoomMonitoring> roomsMonitoring;

    public void addNewRoom(RoomMonitoring monitoring) {
        this.roomsMonitoring.add(monitoring);
    }

    public void removeRoom(RoomMonitoring monitoring) {
        this.roomsMonitoring.remove(monitoring);
    }

    public boolean containsRoom(RoomMonitoring roomMonitoring) {
        return this.roomsMonitoring.contains(roomMonitoring);
    }
}
