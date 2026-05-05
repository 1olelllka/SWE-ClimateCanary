package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "tips", uniqueConstraints = @UniqueConstraint(
        columnNames = {"violationType", "violatedSensor", "violationStatus"}
))
public class Tip {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String msg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationType violationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolatedSensor violatedSensor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarningStatus violationStatus;

    @OneToMany(mappedBy = "tip", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Warnings> warnings = new ArrayList<>();

    public void addNewWarning(Warnings warnings) {
        this.warnings.add(warnings);
    }

    public void removeWarning(Warnings warnings) {
        this.warnings.remove(warnings);
    }
}
