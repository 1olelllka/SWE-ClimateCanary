package at.qe.skeleton.model;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "pollution_limit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PollutionLimit extends LimitValues {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @OneToOne(mappedBy = "polLimit", fetch = FetchType.LAZY)
    private RoomMonitoring roomMonitoring;
}
