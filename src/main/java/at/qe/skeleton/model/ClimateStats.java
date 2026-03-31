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
public class ClimateStats {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private int humVal;

    @Column(nullable = false)
    private int pollVal;

    @Column(nullable = false)
    private int tempVal;

    @Column(nullable = false)
    private LocalDateTime date;
}
