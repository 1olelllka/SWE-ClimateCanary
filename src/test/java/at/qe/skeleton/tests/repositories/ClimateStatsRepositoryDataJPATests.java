package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.ClimateStats;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.repositories.ClimateStatsRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("ClimateStatsRepository")
class ClimateStatsRepositoryDataJPATests {

    @Autowired TestEntityManager em;
    @Autowired ClimateStatsRepository repository;

    private RoomMonitoring room;
    private final LocalDateTime base = LocalDateTime.of(2024, 6, 1, 12, 0);

    @BeforeEach
    void setUp() {
        room = em.persist(RoomMonitoring.builder()
                .roomId(UUID.randomUUID())
                .roomNumber("A101")
                .build());
    }


    @Nested
    @DisplayName("findTopByRoomMonitoring_RoomIdOrderByDateDesc")
    class FindTop {

        @Test
        @DisplayName("returns the latest entry when multiple exist")
        void returnsLatest() {
            persist(base.minusHours(2), 20, 50, 300);
            persist(base,              22, 55, 400); // latest
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
            persist(base,             20, 50, 300);
            persist(base.plusHours(3), 21, 52, 320);
            em.flush();

            List<ClimateStats> result = repository.findByRoomMonitoring_RoomIdAndDateBetween(
                    room.getRoomId(), base, base.plusHours(3));

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no entries fall in range")
        void returnsEmptyForEmptyRange() {
            persist(base.minusDays(5), 20, 50, 300);
            em.flush();

            List<ClimateStats> result = repository.findByRoomMonitoring_RoomIdAndDateBetween(
                    room.getRoomId(), base, base.plusDays(1));

            assertThat(result).isEmpty();
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

            List<ClimateStats> result = repository.findByRoomMonitoring_RoomIdAndDateBetween(
                    room.getRoomId(), base.minusDays(1), base.plusDays(1));

            assertThat(result).isEmpty();
        }
    }

    private void persist(LocalDateTime date, double temp, double hum, double poll) {
        em.persist(ClimateStats.builder()
                .date(date).tempVal(temp).humVal(hum).pollVal(poll)
                .roomMonitoring(room).build());
    }
}
