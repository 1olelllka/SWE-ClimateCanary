package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
    @Column(unique = true)
    private String name;
    @ElementCollection(targetClass = Permission.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "Role_RolePermissions")
    @Enumerated(EnumType.STRING)
    private Set<Permission> permissions;
    @ManyToMany(mappedBy = "userRoles")
    @ToString.Exclude
    private Set<Userx> users = new HashSet<>();
}
