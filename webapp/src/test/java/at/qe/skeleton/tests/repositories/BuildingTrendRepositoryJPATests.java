package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.BuildingTrend;
import at.qe.skeleton.model.Trend;
import at.qe.skeleton.repositories.BuildingTrendRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BuildingTrendRepositoryJPATests {

    @Autowired
    TestEntityManager em;

    @Autowired
    BuildingTrendRepository trendRepository;

    private UUID dept1Id;
    private UUID dept2Id;

    @BeforeEach
    void setUp() {
        dept1Id = UUID.randomUUID();
        dept2Id = UUID.randomUUID();

        em.persist(BuildingTrend.builder()
                .departmentId(dept1Id)
                .departmentName("Engineering")
                .trend(Trend.UP)
                .value(10.0)
                .build());

        em.persist(BuildingTrend.builder()
                .departmentId(dept1Id)
                .departmentName("Engineering")
                .trend(Trend.DOWN)
                .value(5.0)
                .build());

        em.persist(BuildingTrend.builder()
                .departmentId(dept1Id)
                .departmentName("Engineering")
                .trend(Trend.STABLE)
                .value(7.0)
                .build());

        em.persist(BuildingTrend.builder()
                .departmentId(dept2Id)
                .departmentName("HR")
                .trend(Trend.UP)
                .value(3.0)
                .build());

        em.flush();
    }

    @Test
    void testFindFirstByOrderByDateDesc_returnsMostRecent() {
        BuildingTrend result = trendRepository.findFirstByDepartmentIdOrderByDateDesc(dept1Id);
        assertNotNull(result);
        assertEquals(LocalDate.now(), result.getDate());
    }

    @Test
    void testFindAllByDepartmentIdAndDateBetween_returnsCorrectRange() {
        List<BuildingTrend> result = trendRepository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(
                dept1Id, LocalDate.now().minusDays(1), LocalDate.now());

        assertEquals(3, result.size());
    }

    @Test
    void testFindAllByDepartmentIdAndDateBetween_noResults() {
        List<BuildingTrend> result = trendRepository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(
                dept1Id, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));

        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteAllByDepartmentId_deletesOnlyTargetDepartment() {
        trendRepository.deleteAllByDepartmentId(dept1Id);
        em.flush();
        em.clear();

        List<BuildingTrend> remaining = trendRepository.findAll();
        assertEquals(1, remaining.size());
        assertEquals(dept2Id, remaining.get(0).getDepartmentId());
    }

    @Test
    void testDeleteAllByDepartmentId_nonExistentId_doesNothing() {
        trendRepository.deleteAllByDepartmentId(UUID.randomUUID());
        em.flush();

        assertEquals(4, trendRepository.findAll().size());
    }
}