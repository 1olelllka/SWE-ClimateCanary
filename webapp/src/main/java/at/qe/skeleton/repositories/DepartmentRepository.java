package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByName(String name);
    boolean existsByNameAndBuildingId(String name, UUID buildingId);
    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.rooms")
    List<Department> findAllWithRooms();
}
