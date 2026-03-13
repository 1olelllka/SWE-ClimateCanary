package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Userx;
import at.qe.skeleton.model.UserxRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Userx} entities.
 *
 * This class is part of the skeleton project provided for students of the
* course "Software Engineering" offered by Innsbruck University.
 */
public interface UserxRepository extends AbstractRepository<Userx, Long> {

    Optional<Userx> findFirstByUsername(String username);

    List<Userx> findByUsernameContaining(String username);

    @Query("SELECT u FROM Userx u WHERE CONCAT(u.firstName, ' ', u.lastName) = :wholeName")
    List<Userx> findByWholeNameConcat(@Param("wholeName") String wholeName);

    @Query("SELECT u FROM Userx u WHERE :role MEMBER OF u.roles")
    List<Userx> findByRole(@Param("role") UserxRole role);

    boolean existsByUsername(String username);

}
