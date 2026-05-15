package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Builder
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(columnNames = {"room_number", "department_id"}))
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

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private Integer defaultPeopleCnt;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToMany(mappedBy = "myRoom", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Userx> users = new HashSet<>();
}
