package at.qe.skeleton.repositories;

import at.qe.skeleton.model.AggregatedStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AggregatedStatsRepository extends JpaRepository<AggregatedStats, Integer> {
    Optional<AggregatedStats> findByRoomIdAndDate(UUID roomId, LocalDate date);

    List<AggregatedStats> findByRoomIdAndDateBetween(UUID roomId, LocalDate from, LocalDate to);
}
