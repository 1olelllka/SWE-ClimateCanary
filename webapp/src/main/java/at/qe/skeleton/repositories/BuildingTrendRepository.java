package at.qe.skeleton.repositories;

import at.qe.skeleton.model.BuildingTrend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BuildingTrendRepository extends JpaRepository<BuildingTrend, UUID> {
    BuildingTrend findFirstByOrderByDateDesc();
    List<BuildingTrend> findAllByDepartmentIdAndDateBetweenOrderByDateAsc(UUID departmentId, LocalDate start, LocalDate end);
    void deleteAllByDepartmentId(UUID id);
}
