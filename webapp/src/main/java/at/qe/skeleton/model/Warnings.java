package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "warnings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Warnings {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private double triggeredValue;

    @Column(nullable = false)
    private double activeLimitAtTime;

    @Column(nullable = false)
    private UUID sensorWriteId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WarningStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MeasurementType measurementType;

    @ManyToOne(fetch = FetchType.LAZY)
    private Tip tip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_monitoring_id", nullable = false)
    private RoomMonitoring roomMonitoring;

    // active means not yet resolved
    public boolean isActive() {
        return resolvedAt == null;
    }
}
