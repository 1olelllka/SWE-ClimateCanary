package at.qe.skeleton.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Entity representing users.
 *
 * This class is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
@Entity
public class Userx implements Persistable<Long>, Serializable, Comparable<Userx>, UserDetails {

  @Serial
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  private Userx createUser;
  @Column(nullable = false)
  @CreationTimestamp
  private LocalDateTime createDate;
  @ManyToOne(optional = true)
  private Userx updateUser;
  @UpdateTimestamp
  private LocalDateTime updateDate;
  
  @Column(unique = true, nullable = false, length = 100)
  private String username;
  private String password;
  
  private String firstName;
  private String lastName;
  private String email;
  private String phone;
  
  @ElementCollection(targetClass = UserxRole.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "Userx_UserxRole")
  @Enumerated(EnumType.STRING)
  private Set<UserxRole> roles;

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
    return getRoles();
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Set<UserxRole> getRoles() {
    return roles;
  }

  public void setRoles(Set<UserxRole> roles) {
    this.roles = roles;
  }

  public Userx getCreateUser() {
    return createUser;
  }

  public void setCreateUser(Userx createUser) {
    this.createUser = createUser;
  }

  public LocalDateTime getCreateDate() {
    return createDate;
  }

  public void setCreateDate(LocalDateTime createDate) {
    this.createDate = createDate;
  }

  public Userx getUpdateUser() {
    return updateUser;
  }

  public void setUpdateUser(Userx updateUser) {
    this.updateUser = updateUser;
  }

  public LocalDateTime getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(LocalDateTime updateDate) {
    this.updateDate = updateDate;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 59 * hash + Objects.hashCode(this.getId());
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Userx other)) {
      return false;
    }
      return Objects.equals(this.getId(), other.getId());
  }

  @Override
  public String toString() {
    return "at.qe.skeleton.model.User[ id=" + username + " ]";
  }

  @Override
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Override
  public boolean isNew() {
    return (null == id);
  }

  @Override
  public int compareTo(Userx o) {
    return this.id.compareTo(o.getId());
  }

}