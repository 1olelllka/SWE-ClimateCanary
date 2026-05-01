package at.qe.skeleton.repositories;


import at.qe.skeleton.model.Warnings;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WarningRepository extends JpaRepository<Warnings, UUID> {

    // all active warnings for a room (resolvedAt IS NULL = active)
    List<Warnings> findByRoomMonitoring_RoomIdAndResolvedAtIsNull(UUID roomId);

    // all active warnings across all rooms
    @Query("SELECT w FROM Warnings w WHERE w.resolvedAt IS NULL")
    List<Warnings> findAllActive();

    // all historical warnings for a room (active + resolved)
    List<Warnings> findByRoomMonitoring_RoomId(UUID roomId);

    List<Warnings> findByRoomMonitoring_RoomIdAndCreatedAtBetween(UUID roomId, LocalDateTime startDate, LocalDateTime endDate);
}
