package at.qe.skeleton.repositories;

import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.model.Warnings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WarningRepository extends JpaRepository<Warnings, UUID> {
    List<Warnings> findByRoom(UUID roomId);

    List<Warnings> findByStatus(WarningStatus status);

    List<Warnings> findByRoomAndStatus(
            UUID roomId,
            WarningStatus status
    );

    @Query("SELECT w FROM Warnings w WHERE w.roomMonitoring.roomId = :roomId " +
            "AND w.createdAt BETWEEN :from AND :to")
    List<Warnings> findByRoomAndDateRange(
            @Param("roomId") UUID roomId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
