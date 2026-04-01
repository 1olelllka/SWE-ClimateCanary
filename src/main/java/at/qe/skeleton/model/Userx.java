package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Userx implements Comparable<Userx>, UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;
  @Column(nullable = false)
  @CreationTimestamp
  private LocalDateTime createDate;
  @UpdateTimestamp
  private LocalDateTime updateDate;
  
  @Column(unique = true, nullable = false, length = 100)
  private String username;
  private String password;
  
  private String firstName;
  private String lastName;
  private LocalDateTime snoozedWarningsUntil;

  @ManyToMany
  @JoinTable(name = "User_UserRole",
          inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
          joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id")
  )
  private Set<UserRole> userRoles;

  boolean enabled;

  @Override
  public boolean isAccountNonExpired() {
    return UserDetails.super.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return UserDetails.super.isAccountNonLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return UserDetails.super.isCredentialsNonExpired();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Set<GrantedAuthority> authorities = new HashSet<>();
    for (UserRole role : this.userRoles) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
      for (Permission permission : role.getPermissions()) {
        authorities.add(new SimpleGrantedAuthority(permission.name()));
      }
    }
    return authorities;
  }

  @Override
  public String getPassword() {
    return this.password;
  }

  @Override
  public String getUsername() {
    return this.username;
  }

  @Override
  public int compareTo(Userx o) {
    return this.id.compareTo(o.id);
  }

}