package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FormulaWeights {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    @Column(nullable = false)
    Double tempWeight;
    @Column(nullable = false)
    Double co2Weight;
    @Column(nullable = false)
    Double humWeight;
    @Column(nullable = false)
    LocalDateTime modifiedAt;
}
