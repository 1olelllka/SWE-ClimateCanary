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

    // find all by room
    List<ClimateStats> findAllByRoom(UUID roomId);

    // time based
    List<ClimateStats> findAllByRoomAndDateBetween(UUID roomId, LocalDateTime from, LocalDateTime to);
    List<ClimateStats> findAllByDateBetween(LocalDateTime from, LocalDateTime to);

}