package at.qe.skeleton.repositories;

import at.qe.skeleton.model.ClimateStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClimateStatsRepository extends JpaRepository<ClimateStats, UUID> {
    Optional<ClimateStats> findByRoom(UUID roomId);

    List<ClimateStats> findByRoomAndDate(UUID roomId, LocalDateTime from, LocalDateTime to);
}