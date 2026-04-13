package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.AggregatedStats;
import at.qe.skeleton.repositories.AggregatedStatsRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("AggregatedStatsRepository")
class AggregatedStatsRepositoryDataJPATests {

    @Autowired TestEntityManager em;
    @Autowired AggregatedStatsRepository repository;

    private final UUID roomId = UUID.randomUUID();
    private final LocalDate base = LocalDate.of(2024, 6, 15);


    @Nested
    @DisplayName("findByRoomIdAndDateBetween")
    class FindByDateRange {

        @Test
        @DisplayName("returns only entries within the inclusive range")
        void returnsEntriesInRange() {
            persist(roomId, base.minusDays(1), 18, 45, 250); // before
            persist(roomId, base,              20, 50, 300); // in range
            persist(roomId, base.plusDays(1),  21, 52, 310); // in range
            persist(roomId, base.plusDays(2),  22, 55, 400); // after
            em.flush();

            List<AggregatedStats> result = repository.findByRoomIdAndDateBetween(
                    roomId, base, base.plusDays(1));

            assertThat(result).hasSize(2)
                    .extracting(AggregatedStats::getDate)
                    .containsExactlyInAnyOrder(base, base.plusDays(1));
        }

        @Test
        @DisplayName("includes boundary dates (inclusive on both ends)")
        void includesBoundaries() {
            persist(roomId, base,              20, 50, 300);
            persist(roomId, base.plusDays(3),  22, 55, 400);
            em.flush();

            List<AggregatedStats> result = repository.findByRoomIdAndDateBetween(
                    roomId, base, base.plusDays(3));

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no entries fall in range")
        void returnsEmptyWhenNoMatch() {
            persist(roomId, base.minusDays(10), 20, 50, 300);
            em.flush();

            List<AggregatedStats> result = repository.findByRoomIdAndDateBetween(
                    roomId, base, base.plusDays(7));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("does not return entries from other rooms")
        void ignoresOtherRooms() {
            persist(UUID.randomUUID(), base, 30, 60, 500);
            em.flush();

            List<AggregatedStats> result = repository.findByRoomIdAndDateBetween(
                    roomId, base.minusDays(1), base.plusDays(1));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns all matching entries when range spans entire dataset")
        void returnsAllInWideRange() {
            persist(roomId, base,              20, 50, 300);
            persist(roomId, base.plusDays(1),  21, 52, 310);
            persist(roomId, base.plusDays(2),  22, 54, 320);
            em.flush();

            List<AggregatedStats> result = repository.findByRoomIdAndDateBetween(
                    roomId, base.minusDays(1), base.plusDays(10));

            assertThat(result).hasSize(3);
        }
    }

    private void persist(UUID rId, LocalDate date, float temp, float hum, float co2) {
        em.persist(AggregatedStats.builder()
                .roomId(rId).date(date)
                .avgTemp(temp).avgHumidity(hum).avgCO2(co2)
                .build());
    }
}
