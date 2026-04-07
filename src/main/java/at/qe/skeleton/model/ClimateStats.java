package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "climate_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClimateStats {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private int humVal;

    @Column(nullable = false)
    private int pollVal;

    @Column(nullable = false)
    private int tempVal;

    @Column(nullable = false)
    private LocalDateTime date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_monitoring_id", nullable = false)
    private RoomMonitoring roomMonitoring;
}
