package at.qe.skeleton.repositories;

import at.qe.skeleton.model.ClimateStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClimateStatsRepository extends JpaRepository<ClimateStats, UUID> {
    Optional<ClimateStats> findTopByRoomMonitoring_RoomIdOrderByDateDesc(UUID roomId);

    List<ClimateStats> findByRoomMonitoring_RoomIdAndDateBetween(UUID roomId, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            SELECT CAST(cs.date AS localdate)       AS day,
                   AVG(cs.tempVal)                  AS avgTemp,
                   AVG(cs.humVal)                   AS avgHumidity,
                   AVG(cs.pollVal)                  AS avgCO2
            FROM ClimateStats cs
            WHERE cs.roomMonitoring.roomId = :roomId
            GROUP BY CAST(cs.date AS localdate)
            ORDER BY day
            """)
    List<Object[]> findDailyAveragesByRoomId(@Param("roomId") UUID roomId);

    @Query("""
            SELECT CAST(MIN(cs.date) AS localdate)  AS weekStart,
                   AVG(cs.tempVal)                  AS avgTemp,
                   AVG(cs.humVal)                   AS avgHumidity,
                   AVG(cs.pollVal)                  AS avgCO2
            FROM ClimateStats cs
            WHERE cs.roomMonitoring.roomId = :roomId
            GROUP BY YEAR(cs.date), WEEK(cs.date)
            ORDER BY weekStart
            """)
    List<Object[]> findWeeklyAveragesByRoomId(@Param("roomId") UUID roomId);
}