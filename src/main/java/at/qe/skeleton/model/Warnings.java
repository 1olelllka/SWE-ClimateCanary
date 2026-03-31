package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "warnings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warnings {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private int triggeredValue;

    @Column(nullable = false)
    private int activeLimitAtTime;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WarningStatus status;
}
