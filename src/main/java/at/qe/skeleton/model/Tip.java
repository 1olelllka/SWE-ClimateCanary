package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tip {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String msg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationType violationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolatedSensor violatedSensor;
}
