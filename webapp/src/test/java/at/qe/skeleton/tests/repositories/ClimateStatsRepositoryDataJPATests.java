package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.ClimateStats;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.repositories.ClimateStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@DisplayName("ClimateStatsRepository")
class ClimateStatsRepositoryDataJPATests {

    @Autowired TestEntityManager em;
    @Autowired ClimateStatsRepository repository;

    private RoomMonitoring room;

    // Fixed UTC base: 2024-06-01T12:00:00Z
    private final OffsetDateTime base = OffsetDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        room = em.persist(RoomMonitoring.builder()
                .roomId(UUID.randomUUID())
                .roomNumber("A101")
                .build());
    }

    // ── findTopByRoomMonitoring_RoomIdOrderByDateDesc ────────────────────────────

    @Nested
    @DisplayName("findTopByRoomMonitoring_RoomIdOrderByDateDesc")
    class FindTop {

        @Test
        @DisplayName("returns the latest entry when multiple exist")
        void returnsLatest() {
            persist(base.minusHours(2), 20, 50, 300);
            persist(base,               22, 55, 400); // latest
            persist(base.minusHours(1), 21, 52, 350);
            em.flush();

            Optional<ClimateStats> result =
                    repository.findTopByRoomMonitoring_RoomIdOrderByDateDesc(room.getRoomId());

            assertThat(result).isPresent();
            assertThat(result.get().getDate()).isEqualTo(base);
            assertThat(result.get().getTempVal()).isEqualTo(22);
        }

        @Test
        @DisplayName("returns the single entry when only one exists")
        void returnsSingleEntry() {
            persist(base, 20, 50, 300);
            em.flush();

            assertThat(repository.findTopByRoomMonitoring_RoomIdOrderByDateDesc(room.getRoomId()))
                    .isPresent();
        }

        @Test
        @DisplayName("returns empty when room has no data")
        void returnsEmptyForUnknownRoom() {
            assertThat(repository.findTopByRoomMonitoring_RoomIdOrderByDateDesc(UUID.randomUUID()))
                    .isEmpty();
        }

        @Test
        @DisplayName("does not return data belonging to a different room")
        void ignoresOtherRooms() {
            RoomMonitoring other = em.persist(RoomMonitoring.builder()
                    .roomId(UUID.randomUUID()).roomNumber("B202").build());
            em.persist(ClimateStats.builder()
                    .date(base).tempVal(30).humVal(60).pollVal(500)
                    .roomMonitoring(other).build());
            em.flush();

            assertThat(repository.findTopByRoomMonitoring_RoomIdOrderByDateDesc(room.getRoomId()))
                    .isEmpty();
        }
    }

    // ── findByRoomMonitoring_RoomIdAndDateBetween ────────────────────────────────

    @Nested
    @DisplayName("findByRoomMonitoring_RoomIdAndDateBetween")
    class FindByDateRange {

        @Test
        @DisplayName("returns only entries within the inclusive range")
        void returnsEntriesInRange() {
            persist(base.minusDays(1), 18, 45, 250); // before range
            persist(base,              20, 50, 300); // in range
            persist(base.plusHours(6), 21, 52, 320); // in range
            persist(base.plusDays(1),  22, 55, 400); // after range
            em.flush();

            List<ClimateStats> result = repository.findByRoomMonitoring_RoomIdAndDateBetween(
                    room.getRoomId(), base, base.plusHours(6));

            assertThat(result).hasSize(2)
                    .extracting(ClimateStats::getTempVal)
                    .containsExactlyInAnyOrder(20.0, 21.0);
        }

        @Test
        @DisplayName("includes boundary values (inclusive on both ends)")
        void includesBoundaries() {
            persist(base,              20, 50, 300);
            persist(base.plusHours(3), 21, 52, 320);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndDateBetween(
                    room.getRoomId(), base, base.plusHours(3)))
                    .hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no entries fall in range")
        void returnsEmptyForEmptyRange() {
            persist(base.minusDays(5), 20, 50, 300);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndDateBetween(
                    room.getRoomId(), base, base.plusDays(1)))
                    .isEmpty();
        }

        @Test
        @DisplayName("does not return entries from other rooms")
        void ignoresOtherRooms() {
            RoomMonitoring other = em.persist(RoomMonitoring.builder()
                    .roomId(UUID.randomUUID()).roomNumber("C303").build());
            em.persist(ClimateStats.builder()
                    .date(base).tempVal(30).humVal(60).pollVal(500)
                    .roomMonitoring(other).build());
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndDateBetween(
                    room.getRoomId(), base.minusDays(1), base.plusDays(1)))
                    .isEmpty();
        }
    }

    // ── findDailyAveragesByRoomId ────────────────────────────────────────────────

    @Nested
    @DisplayName("findDailyAveragesByRoomId")
    class FindDailyAverages {

        @Test
        @DisplayName("returns one row per day with correct averages")
        void returnsOneRowPerDay() {
            // Day 1: two entries
            persist(base,               20, 50, 300);
            persist(base.plusHours(4),  24, 60, 500);
            // Day 2: one entry
            persist(base.plusDays(1),   30, 70, 600);
            em.flush();

            List<Object[]> rows = repository.findDailyAveragesByRoomId(room.getRoomId());

            assertThat(rows).hasSize(2);

            // Day 1 averages: temp=(20+24)/2=22, hum=(50+60)/2=55, co2=(300+500)/2=400
            Object[] day1 = rows.get(0);
            assertThat(day1[0]).isEqualTo(LocalDate.of(2024, 6, 1));
            assertThat((Double) day1[1]).isCloseTo(22.0, within(0.01));
            assertThat((Double) day1[2]).isCloseTo(55.0, within(0.01));
            assertThat((Double) day1[3]).isCloseTo(400.0, within(0.01));

            // Day 2 averages: single entry
            Object[] day2 = rows.get(1);
            assertThat(day2[0]).isEqualTo(LocalDate.of(2024, 6, 2));
            assertThat((Double) day2[1]).isCloseTo(30.0, within(0.01));
        }

        @Test
        @DisplayName("returns empty list when room has no data")
        void returnsEmptyForUnknownRoom() {
            assertThat(repository.findDailyAveragesByRoomId(UUID.randomUUID())).isEmpty();
        }

        @Test
        @DisplayName("does not include data from other rooms")
        void ignoresOtherRooms() {
            RoomMonitoring other = em.persist(RoomMonitoring.builder()
                    .roomId(UUID.randomUUID()).roomNumber("D404").build());
            em.persist(ClimateStats.builder()
                    .date(base).tempVal(99).humVal(99).pollVal(999)
                    .roomMonitoring(other).build());
            em.flush();

            assertThat(repository.findDailyAveragesByRoomId(room.getRoomId())).isEmpty();
        }

        @Test
        @DisplayName("rows are ordered by day ascending")
        void orderedAscending() {
            persist(base.plusDays(2), 10, 10, 10);
            persist(base,             20, 20, 20);
            persist(base.plusDays(1), 30, 30, 30);
            em.flush();

            List<Object[]> rows = repository.findDailyAveragesByRoomId(room.getRoomId());

            assertThat(rows).hasSize(3);
            assertThat(rows.get(0)[0]).isEqualTo(LocalDate.of(2024, 6, 1));
            assertThat(rows.get(1)[0]).isEqualTo(LocalDate.of(2024, 6, 2));
            assertThat(rows.get(2)[0]).isEqualTo(LocalDate.of(2024, 6, 3));
        }
    }

    // ── findWeeklyAveragesByRoomId ───────────────────────────────────────────────

    @Nested
    @DisplayName("findWeeklyAveragesByRoomId")
    class FindWeeklyAverages {

        @Test
        @DisplayName("returns one row per ISO week with correct averages")
        void returnsOneRowPerWeek() {
            // Week 23 of 2024 (June 3–9)
            OffsetDateTime week23 = OffsetDateTime.of(2024, 6, 3, 12, 0, 0, 0, ZoneOffset.UTC);
            persist(week23,              20, 50, 300);
            persist(week23.plusDays(1),  24, 60, 500);

            // Week 24 of 2024 (June 10–16)
            OffsetDateTime week24 = OffsetDateTime.of(2024, 6, 10, 12, 0, 0, 0, ZoneOffset.UTC);
            persist(week24, 30, 70, 600);
            em.flush();

            List<Object[]> rows = repository.findWeeklyAveragesByRoomId(room.getRoomId());

            assertThat(rows).hasSize(2);

            // Week 23: temp avg = 22, hum avg = 55, co2 avg = 400
            assertThat((Double) rows.get(0)[1]).isCloseTo(22.0, within(0.01));
            assertThat((Double) rows.get(0)[2]).isCloseTo(55.0, within(0.01));
            assertThat((Double) rows.get(0)[3]).isCloseTo(400.0, within(0.01));

            // Week 24: single entry
            assertThat((Double) rows.get(1)[1]).isCloseTo(30.0, within(0.01));
        }

        @Test
        @DisplayName("returns empty list when room has no data")
        void returnsEmptyForUnknownRoom() {
            assertThat(repository.findWeeklyAveragesByRoomId(UUID.randomUUID())).isEmpty();
        }

        @Test
        @DisplayName("does not include data from other rooms")
        void ignoresOtherRooms() {
            RoomMonitoring other = em.persist(RoomMonitoring.builder()
                    .roomId(UUID.randomUUID()).roomNumber("E505").build());
            em.persist(ClimateStats.builder()
                    .date(base).tempVal(99).humVal(99).pollVal(999)
                    .roomMonitoring(other).build());
            em.flush();

            assertThat(repository.findWeeklyAveragesByRoomId(room.getRoomId())).isEmpty();
        }

        @Test
        @DisplayName("rows are ordered by week start ascending")
        void orderedAscending() {
            OffsetDateTime week25 = OffsetDateTime.of(2024, 6, 17, 12, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime week23 = OffsetDateTime.of(2024, 6,  3, 12, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime week24 = OffsetDateTime.of(2024, 6, 10, 12, 0, 0, 0, ZoneOffset.UTC);

            persist(week25, 10, 10, 10);
            persist(week23, 20, 20, 20);
            persist(week24, 30, 30, 30);
            em.flush();

            List<Object[]> rows = repository.findWeeklyAveragesByRoomId(room.getRoomId());

            assertThat(rows).hasSize(3);
            // weekStart is the MIN(date) cast to localdate within each group
            assertThat((LocalDate) rows.get(0)[0])
                    .isBefore((LocalDate) rows.get(1)[0]);
            assertThat((LocalDate) rows.get(1)[0])
                    .isBefore((LocalDate) rows.get(2)[0]);
        }
    }

    // ── helper ───────────────────────────────────────────────────────────────────

    private void persist(OffsetDateTime date, double temp, double hum, double poll) {
        em.persist(ClimateStats.builder()
                .date(date).tempVal(temp).humVal(hum).pollVal(poll)
                .roomMonitoring(room).build());
    }
}