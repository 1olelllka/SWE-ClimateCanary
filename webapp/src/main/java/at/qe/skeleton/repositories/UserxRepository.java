package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Userx;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserxRepository extends JpaRepository<Userx, UUID> {

    Optional<Userx> findFirstByUsername(String username);

    @Query("SELECT u FROM Userx u JOIN u.userRoles r WHERE r.name = :roleName")
    List<Userx> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT u FROM Userx u LEFT JOIN FETCH u.userRoles r LEFT JOIN FETCH r.permissions LEFT JOIN FETCH u.myRoom room LEFT JOIN FETCH room.department WHERE u.username = :username")
    Optional<Userx> findByUsernameWithRoles(@Param("username") String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM Userx u WHERE u.myRoom.department.id = :id")
    List<Userx> findAllByDepartment(@Param("id") UUID id);
}
