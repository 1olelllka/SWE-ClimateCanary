package at.qe.skeleton.repositories;

import at.qe.skeleton.model.AggregatedStats;
import at.qe.skeleton.model.Granularity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AggregatedStatsRepository extends JpaRepository<AggregatedStats, Integer> {
    List<AggregatedStats> findByRoomIdAndDateBetween(UUID roomId, LocalDate from, LocalDate to);
    List<AggregatedStats> findByRoomIdAndDateBetweenAndGranularity(UUID roomId, LocalDate from, LocalDate to, Granularity granularity);
    boolean existsByRoomIdAndDateAndGranularity(UUID roomId, LocalDate date, Granularity granularity);
    AggregatedStats findFirstByRoomIdAndGranularityOrderByDateDesc(UUID roomId, Granularity granularity);
}
