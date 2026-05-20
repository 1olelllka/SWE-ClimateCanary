package at.qe.skeleton.repositories;

import at.qe.skeleton.model.AggregatedDepartmentStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AggregatedDepartmentStatsRepository extends JpaRepository<AggregatedDepartmentStats, UUID> {
    boolean existsByDepartmentIdAndDate(UUID departmentId, LocalDate date);
    Optional<AggregatedDepartmentStats> findFirstByDepartmentIdOrderByDateDesc(UUID roomId);
    List<AggregatedDepartmentStats> findAllByDepartmentIdAndDateBetweenOrderByDateAsc(UUID departmentId, LocalDate startDate, LocalDate endDate);
}
