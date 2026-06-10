package at.qe.skeleton.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    @Builder.Default
    Double tempWeight = 0.4;
    @Builder.Default
    Double co2Weight = 0.3;
    @Builder.Default
    Double humWeight = 0.3;
    LocalDateTime modifiedAt;
}
