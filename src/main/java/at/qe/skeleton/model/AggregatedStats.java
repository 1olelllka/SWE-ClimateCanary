package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "aggregated_stats")
public class AggregatedStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private float avgTemp;

    @Column(nullable = false)
    private float avgHumidity;

    @Column(nullable = false)
    private float avgCO2;
}
