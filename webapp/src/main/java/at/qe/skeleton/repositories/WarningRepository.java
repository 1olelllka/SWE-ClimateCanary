package at.qe.skeleton.repositories;


import at.qe.skeleton.model.MeasurementType;
import at.qe.skeleton.model.Warnings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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

    @Query("SELECT w FROM Warnings w WHERE w.resolvedAt IS NULL AND w.measurementType = :type AND w.roomMonitoring.roomId = :roomId")
    List<Warnings> findAllByRoomAndActiveByType(UUID roomId, MeasurementType type);

    List<Warnings> findByRoomMonitoring_RoomIdInAndResolvedAtIsNull(List<UUID> rooms);
    List<Warnings> findByRoomMonitoring_RoomIdInAndResolvedAtIsNullAndCreatedAtBetween(List<UUID> rooms, LocalDateTime startDate, LocalDateTime endDate);
    List<Warnings> findByRoomMonitoring_RoomIdInAndCreatedAtBetween(List<UUID> rooms, LocalDateTime startDate, LocalDateTime endDate);

    // all historical warnings for a room (active + resolved)
    List<Warnings> findByRoomMonitoring_RoomId(UUID roomId);

    List<Warnings> findByRoomMonitoring_RoomIdAndCreatedAtBetween(UUID roomId, LocalDateTime startDate, LocalDateTime endDate);
}
