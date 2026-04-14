package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Absence {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    private AbsenceType typeOfAbsence;
    private AbsenceStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private UUID assignedTo;
    private String comment;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private Userx user;
}
