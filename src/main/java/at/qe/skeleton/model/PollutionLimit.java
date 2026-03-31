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
public class PollutionLimit extends LimitValues {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
}
