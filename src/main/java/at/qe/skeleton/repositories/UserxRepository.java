package at.qe.skeleton.repositories;

import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.model.Userx;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserxRepository extends JpaRepository<Userx, UUID> {

    Optional<Userx> findFirstByUsername(String username);

    List<Userx> findByUsernameContaining(String username);

    @Query("SELECT u FROM Userx u WHERE CONCAT(u.firstName, ' ', u.lastName) = :wholeName")
    List<Userx> findByWholeNameConcat(@Param("wholeName") String wholeName);

    @Query("SELECT u FROM Userx u WHERE :role MEMBER OF u.userRoles")
    List<Userx> findByRole(@Param("role") UserRole role);

    boolean existsByUsername(String username);

}
