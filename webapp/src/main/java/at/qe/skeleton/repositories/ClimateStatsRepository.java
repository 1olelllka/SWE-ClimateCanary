package at.qe.skeleton.repositories;

import at.qe.skeleton.model.ClimateStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClimateStatsRepository extends JpaRepository<ClimateStats, UUID> {
    Optional<ClimateStats> findTopByRoomMonitoring_RoomIdOrderByDateDesc(UUID roomId);

    List<ClimateStats> findByRoomMonitoring_RoomIdAndDateBetween(UUID roomId, OffsetDateTime from, OffsetDateTime to);
}