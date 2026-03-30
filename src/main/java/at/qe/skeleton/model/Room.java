package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Builder
@Table(name = "rooms")
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType roomType;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private int defaultPeopleCnt;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // now without users

}
