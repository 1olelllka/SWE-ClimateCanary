package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
    private double humVal;

    @Column(nullable = false)
    private double pollVal;

    @Column(nullable = false)
    private double tempVal;

    @Column(nullable = false)
    private OffsetDateTime date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_monitoring_id", nullable = false)
    private RoomMonitoring roomMonitoring;
}
