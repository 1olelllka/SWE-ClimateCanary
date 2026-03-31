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
public class HumidityLimit extends LimitValues {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(mappedBy = "humLimit", fetch = FetchType.LAZY)
    private RoomMonitoring roomMonitoring;
}
