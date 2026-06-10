package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BuildingTrend {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    @Column(nullable = false)
    private UUID departmentId;
    @Column(nullable = false)
    private String departmentName;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Trend trend;
    @Column(nullable = false, name = "trend_value")
    private Double value;
    private LocalDate date;
}