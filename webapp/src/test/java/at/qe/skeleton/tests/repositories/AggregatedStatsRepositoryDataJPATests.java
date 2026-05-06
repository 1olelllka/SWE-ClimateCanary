package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.AggregatedStats;
import at.qe.skeleton.model.Granularity;
import at.qe.skeleton.repositories.AggregatedStatsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

    @Nested
    @DisplayName("findByRoomIdAndDateBetweenAndGranularity")
    class FindByDateRangeAndGranularity {

        @Test
        @DisplayName("returns only entries with matching granularity")
        void filtersGranularity() {
            persist(roomId, base,             20, 50, 300, Granularity.DAILY);
            persist(roomId, base.plusDays(1), 21, 52, 310, Granularity.WEEKLY);
            em.flush();

            List<AggregatedStats> result = repository.findByRoomIdAndDateBetweenAndGranularity(
                    roomId, base, base.plusDays(1), Granularity.DAILY);

            assertThat(result).hasSize(1)
                    .extracting(AggregatedStats::getGranularity)
                    .containsOnly(Granularity.DAILY);
        }

        @Test
        @DisplayName("returns empty when granularity does not match any entry in range")
        void returnsEmptyWhenNoGranularityMatch() {
            persist(roomId, base, 20, 50, 300, Granularity.DAILY);
            em.flush();

            List<AggregatedStats> result = repository.findByRoomIdAndDateBetweenAndGranularity(
                    roomId, base, base, Granularity.WEEKLY);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByRoomIdAndDateAndGranularity")
    class ExistsByRoomIdDateGranularity {

        @Test
        @DisplayName("returns true when matching entry exists")
        void returnsTrueWhenExists() {
            persist(roomId, base, 20, 50, 300, Granularity.DAILY);
            em.flush();

            assertThat(repository.existsByRoomIdAndDateAndGranularity(
                    roomId, base, Granularity.DAILY)).isTrue();
        }

        @Test
        @DisplayName("returns false when granularity does not match")
        void returnsFalseForWrongGranularity() {
            persist(roomId, base, 20, 50, 300, Granularity.DAILY);
            em.flush();

            assertThat(repository.existsByRoomIdAndDateAndGranularity(
                    roomId, base, Granularity.WEEKLY)).isFalse();
        }

        @Test
        @DisplayName("returns false when no entry exists for date")
        void returnsFalseWhenMissing() {
            assertThat(repository.existsByRoomIdAndDateAndGranularity(
                    roomId, base, Granularity.DAILY)).isFalse();
        }
    }

    private void persist(UUID rId, LocalDate date, float temp, float hum, float co2) {
        persist(rId, date, temp, hum, co2, Granularity.DAILY);
    }

    private void persist(UUID rId, LocalDate date, float temp, float hum, float co2, Granularity granularity) {
        em.persist(AggregatedStats.builder()
                .roomId(rId).date(date).granularity(granularity)
                .avgTemp(temp).avgHumidity(hum).avgCO2(co2)
                .build());
    }
}
