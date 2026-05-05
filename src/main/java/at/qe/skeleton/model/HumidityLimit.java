package at.qe.skeleton.model;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "humidity_limit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class HumidityLimit extends LimitValues {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @OneToOne(mappedBy = "humLimit", fetch = FetchType.LAZY)
    private RoomMonitoring roomMonitoring;

    @PrePersist
    protected void prePersist() {
        setMinVal(30f);
        setMaxVal(60f);
    }
}
