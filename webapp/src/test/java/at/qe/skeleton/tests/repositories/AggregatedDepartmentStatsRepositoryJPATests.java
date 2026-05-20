package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.AggregatedDepartmentStats;
import at.qe.skeleton.repositories.AggregatedDepartmentStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AggregatedDepartmentStatsRepositoryJPATests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AggregatedDepartmentStatsRepository repository;

    private UUID departmentId;
    private UUID otherDepartmentId;

    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final LocalDate TWO_DAYS_AGO = TODAY.minusDays(2);
    private static final LocalDate LAST_WEEK = TODAY.minusWeeks(1);

    @BeforeEach
    void setUp() {
        departmentId = UUID.randomUUID();
        otherDepartmentId = UUID.randomUUID();
    }

    // --- Helper ---

    private AggregatedDepartmentStats persist(UUID deptId, LocalDate date,
                                              float temp, float humidity, float co2) {
        return entityManager.persistAndFlush(
                AggregatedDepartmentStats.builder()
                        .departmentId(deptId)
                        .date(date)
                        .avgTemp(temp)
                        .avgHumidity(humidity)
                        .avgCO2(co2)
                        .build()
        );
    }

    // -------------------------------------------------------------------------
    // existsByDepartmentIdAndDate
    // -------------------------------------------------------------------------

    @Test
    void existsByDepartmentIdAndDate_returnsTrueWhenRecordExists() {
        persist(departmentId, TODAY, 21.5f, 55.0f, 400.0f);

        boolean exists = repository.existsByDepartmentIdAndDate(departmentId, TODAY);

        assertThat(exists).isTrue();
    }

    @Test
    void existsByDepartmentIdAndDate_returnsFalseWhenNoDepartmentMatch() {
        persist(departmentId, TODAY, 21.5f, 55.0f, 400.0f);

        boolean exists = repository.existsByDepartmentIdAndDate(otherDepartmentId, TODAY);

        assertThat(exists).isFalse();
    }

    @Test
    void existsByDepartmentIdAndDate_returnsFalseWhenNoDateMatch() {
        persist(departmentId, TODAY, 21.5f, 55.0f, 400.0f);

        boolean exists = repository.existsByDepartmentIdAndDate(departmentId, YESTERDAY);

        assertThat(exists).isFalse();
    }

    @Test
    void existsByDepartmentIdAndDate_returnsFalseWhenRepositoryIsEmpty() {
        boolean exists = repository.existsByDepartmentIdAndDate(departmentId, TODAY);

        assertThat(exists).isFalse();
    }

    @Test
    void existsByDepartmentIdAndDate_returnsTrueOnlyForExactDepartmentAndDateCombination() {
        persist(departmentId, TODAY, 21.5f, 55.0f, 400.0f);
        persist(otherDepartmentId, YESTERDAY, 22.0f, 60.0f, 420.0f);

        assertThat(repository.existsByDepartmentIdAndDate(departmentId, TODAY)).isTrue();
        assertThat(repository.existsByDepartmentIdAndDate(departmentId, YESTERDAY)).isFalse();
        assertThat(repository.existsByDepartmentIdAndDate(otherDepartmentId, TODAY)).isFalse();
        assertThat(repository.existsByDepartmentIdAndDate(otherDepartmentId, YESTERDAY)).isTrue();
    }

    // -------------------------------------------------------------------------
    // findFirstByDepartmentIdOrderByDateDesc
    // -------------------------------------------------------------------------

    @Test
    void findFirstByDepartmentIdOrderByDateDesc_returnsLatestRecord() {
        persist(departmentId, TWO_DAYS_AGO, 19.0f, 50.0f, 380.0f);
        persist(departmentId, YESTERDAY,    20.0f, 52.0f, 390.0f);
        persist(departmentId, TODAY,        21.5f, 55.0f, 400.0f);

        Optional<AggregatedDepartmentStats> result =
                repository.findFirstByDepartmentIdOrderByDateDesc(departmentId);

        assertThat(result).isPresent();
        assertThat(result.get().getDate()).isEqualTo(TODAY);
        assertThat(result.get().getAvgTemp()).isEqualTo(21.5f);
    }

    @Test
    void findFirstByDepartmentIdOrderByDateDesc_returnsEmptyWhenNoDepartmentRecords() {
        persist(otherDepartmentId, TODAY, 21.5f, 55.0f, 400.0f);

        Optional<AggregatedDepartmentStats> result =
                repository.findFirstByDepartmentIdOrderByDateDesc(departmentId);

        assertThat(result).isEmpty();
    }

    @Test
    void findFirstByDepartmentIdOrderByDateDesc_returnsEmptyWhenRepositoryIsEmpty() {
        Optional<AggregatedDepartmentStats> result =
                repository.findFirstByDepartmentIdOrderByDateDesc(departmentId);

        assertThat(result).isEmpty();
    }

    @Test
    void findFirstByDepartmentIdOrderByDateDesc_returnsSingleRecordWhenOnlyOneExists() {
        persist(departmentId, TODAY, 21.5f, 55.0f, 400.0f);

        Optional<AggregatedDepartmentStats> result =
                repository.findFirstByDepartmentIdOrderByDateDesc(departmentId);

        assertThat(result).isPresent();
        assertThat(result.get().getDate()).isEqualTo(TODAY);
    }

    @Test
    void findFirstByDepartmentIdOrderByDateDesc_ignoresOtherDepartments() {
        persist(departmentId,      YESTERDAY, 20.0f, 52.0f, 390.0f);
        persist(otherDepartmentId, TODAY,     21.5f, 55.0f, 400.0f);

        Optional<AggregatedDepartmentStats> result =
                repository.findFirstByDepartmentIdOrderByDateDesc(departmentId);

        assertThat(result).isPresent();
        assertThat(result.get().getDate()).isEqualTo(YESTERDAY);
        assertThat(result.get().getDepartmentId()).isEqualTo(departmentId);
    }

    // -------------------------------------------------------------------------
    // findAllByDepartmentIdAndDateBetweenOrderByDateAsc
    // -------------------------------------------------------------------------

    @Test
    void findAllByDepartmentIdAndDateBetweenOrderByDateAsc_returnsRecordsInRange() {
        persist(departmentId, TWO_DAYS_AGO, 19.0f, 50.0f, 380.0f);
        persist(departmentId, YESTERDAY,    20.0f, 52.0f, 390.0f);
        persist(departmentId, TODAY,        21.5f, 55.0f, 400.0f);

        List<AggregatedDepartmentStats> results =
                repository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(
                        departmentId, TWO_DAYS_AGO, TODAY);

        assertThat(results).hasSize(3);
        assertThat(results).extracting(AggregatedDepartmentStats::getDate)
                .containsExactly(TWO_DAYS_AGO, YESTERDAY, TODAY);
    }

    @Test
    void findAllByDepartmentIdAndDateBetweenOrderByDateAsc_isOrderedAscending() {
        persist(departmentId, TODAY,        21.5f, 55.0f, 400.0f);
        persist(departmentId, TWO_DAYS_AGO, 19.0f, 50.0f, 380.0f);
        persist(departmentId, YESTERDAY,    20.0f, 52.0f, 390.0f);

        List<AggregatedDepartmentStats> results =
                repository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(
                        departmentId, TWO_DAYS_AGO, TODAY);

        assertThat(results).extracting(AggregatedDepartmentStats::getDate)
                .containsExactly(TWO_DAYS_AGO, YESTERDAY, TODAY);
    }

    @Test
    void findAllByDepartmentIdAndDateBetweenOrderByDateAsc_excludesRecordsOutsideRange() {
        persist(departmentId, LAST_WEEK,    18.0f, 48.0f, 370.0f);
        persist(departmentId, TWO_DAYS_AGO, 19.0f, 50.0f, 380.0f);
        persist(departmentId, YESTERDAY,    20.0f, 52.0f, 390.0f);
        persist(departmentId, TODAY,        21.5f, 55.0f, 400.0f);

        List<AggregatedDepartmentStats> results =
                repository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(
                        departmentId, TWO_DAYS_AGO, YESTERDAY);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(AggregatedDepartmentStats::getDate)
                .containsExactly(TWO_DAYS_AGO, YESTERDAY);
    }

    @Test
    void findAllByDepartmentIdAndDateBetweenOrderByDateAsc_includesBoundaryDates() {
        persist(departmentId, YESTERDAY, 20.0f, 52.0f, 390.0f);
        persist(departmentId, TODAY,     21.5f, 55.0f, 400.0f);

        List<AggregatedDepartmentStats> results =
                repository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(
                        departmentId, YESTERDAY, TODAY);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getDate()).isEqualTo(YESTERDAY);
        assertThat(results.get(1).getDate()).isEqualTo(TODAY);
    }

    @Test
    void findAllByDepartmentIdAndDateBetweenOrderByDateAsc_returnsEmptyWhenNoRecordsInRange() {
        persist(departmentId, LAST_WEEK, 18.0f, 48.0f, 370.0f);

        List<AggregatedDepartmentStats> results =
                repository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(
                        departmentId, TWO_DAYS_AGO, TODAY);

        assertThat(results).isEmpty();
    }

    @Test
    void findAllByDepartmentIdAndDateBetweenOrderByDateAsc_ignoresOtherDepartments() {
        persist(departmentId,      YESTERDAY, 20.0f, 52.0f, 390.0f);
        persist(otherDepartmentId, TODAY,     21.5f, 55.0f, 400.0f);

        List<AggregatedDepartmentStats> results =
                repository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(
                        departmentId, YESTERDAY, TODAY);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDepartmentId()).isEqualTo(departmentId);
    }

    @Test
    void findAllByDepartmentIdAndDateBetweenOrderByDateAsc_returnsSingleRecordRange() {
        persist(departmentId, TODAY, 21.5f, 55.0f, 400.0f);

        List<AggregatedDepartmentStats> results =
                repository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(
                        departmentId, TODAY, TODAY);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDate()).isEqualTo(TODAY);
    }

    @Test
    void findAllByDepartmentIdAndDateBetweenOrderByDateAsc_returnsCorrectFieldValues() {
        persist(departmentId, TODAY, 23.7f, 61.2f, 415.5f);

        List<AggregatedDepartmentStats> results =
                repository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(
                        departmentId, TODAY, TODAY);

        assertThat(results).hasSize(1);
        AggregatedDepartmentStats stat = results.get(0);
        assertThat(stat.getDepartmentId()).isEqualTo(departmentId);
        assertThat(stat.getDate()).isEqualTo(TODAY);
        assertThat(stat.getAvgTemp()).isEqualTo(23.7f);
        assertThat(stat.getAvgHumidity()).isEqualTo(61.2f);
        assertThat(stat.getAvgCO2()).isEqualTo(415.5f);
    }
}