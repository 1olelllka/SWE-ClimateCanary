package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table(name = "aggregated_department_stats")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AggregatedDepartmentStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private int id;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private float avgTemp;

    @Column(nullable = false)
    private float avgHumidity;

    @Column(nullable = false)
    private float avgCO2;
}
