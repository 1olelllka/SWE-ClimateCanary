package at.qe.skeleton.tests.background;

import at.qe.skeleton.background.TrendJob;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.AggregatedStatsRepository;
import at.qe.skeleton.repositories.BuildingTrendRepository;
import at.qe.skeleton.repositories.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrendJob")
class TrendJobUnitTests {

    @Mock private BuildingTrendRepository    trendRepository;
    @Mock private AggregatedStatsRepository  aggregatedStatsRepository;
    @Mock private DepartmentRepository       departmentRepository;

    @InjectMocks private TrendJob trendJob;

    private UUID       deptId;
    private UUID       roomId;
    private Department department;
    private Room       room;

    @BeforeEach
    void setUp() {
        deptId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        room = new Room();
        room.setId(roomId);
        room.setRoomNumber("101");

        department = new Department();
        department.setId(deptId);
        department.setName("Informatics");
        department.setRooms(List.of(room));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubDepartments() {
        when(departmentRepository.findAllWithRooms()).thenReturn(List.of(department));
    }

    private AggregatedStats statsOf(float temp, float humidity, float co2) {
        return AggregatedStats.builder()
                .avgTemp(temp)
                .avgHumidity(humidity)
                .avgCO2(co2)
                .build();
    }

    private BuildingTrend previousTrendWithValue(double value) {
        return BuildingTrend.builder()
                .value(value)
                .date(LocalDate.now().minusDays(1))
                .build();
    }

    private BuildingTrend capturedTrend() {
        trendJob.trendDaily();
        ArgumentCaptor<BuildingTrend> captor = ArgumentCaptor.forClass(BuildingTrend.class);
        verify(trendRepository).save(captor.capture());
        return captor.getValue();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // trendDaily — trend direction
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("trend direction")
    class TrendDirection {

        @Test
        @DisplayName("UP when calculated value exceeds previous trend value")
        void calculatesUpTrend() {
            // raw = 0.4 * 0.5 + 0.3 * 0.5 + 0.3 * 0.375 = 0.4625
            // value = (1 - 0.4625) * 100 = 53.75
            double expectedValue = 53.75;

            stubDepartments();
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(22f, 50f, 1000f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(previousTrendWithValue(30.0)); // lower → UP

            BuildingTrend saved = capturedTrend();

            assertEquals(deptId,          saved.getDepartmentId());
            assertEquals("Informatics",   saved.getDepartmentName());
            assertEquals(expectedValue,   saved.getValue(), 0.0001);
            assertEquals(Trend.UP,        saved.getTrend());
            assertEquals(LocalDate.now(), saved.getDate());
        }

        @Test
        @DisplayName("DOWN when calculated value is below previous trend value")
        void calculatesDownTrend() {
            // All sensors at minimum → raw = 0.0 → value = 100.0
            // previousTrend = 150.0 > 100.0 → DOWN
            stubDepartments();
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(18f, 30f, 400f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(previousTrendWithValue(150.0)); // higher → DOWN

            assertEquals(Trend.DOWN, capturedTrend().getTrend());
        }

        @Test
        @DisplayName("STABLE when calculated value equals previous trend value")
        void calculatesStableTrend() {
            // All sensors at maximum → raw = 1.0 → value = 0.0
            stubDepartments();
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(26f, 70f, 2000f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(previousTrendWithValue(0.0)); // equal → STABLE

            assertEquals(Trend.STABLE, capturedTrend().getTrend());
        }

        @Test
        @DisplayName("STABLE when no previous trend record exists (first run)")
        void defaultsToStableWhenNoPreviousTrend() {
            stubDepartments();
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(22f, 50f, 1000f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(null); // no history

            assertEquals(Trend.STABLE, capturedTrend().getTrend());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // trendDaily — missing / partial stats
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("missing or partial stats")
    class MissingStats {

        @Test
        @DisplayName("saves trend with value=0 when all rooms have no aggregated stats")
        void allRoomsMissingStats_savesTrendWithZeroValue() {
            stubDepartments();
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(null);
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(null);

            BuildingTrend saved = capturedTrend();

            assertEquals(0.0,          saved.getValue(), 0.0001);
            assertEquals(Trend.STABLE,  saved.getTrend()); // no previous → STABLE
            assertEquals(deptId,        saved.getDepartmentId());
        }

        @Test
        @DisplayName("partial rooms: uses only rooms that have stats; skips rooms without")
        void partialRoomsMissingStats_accumulatesOnlyValidRooms() {
            Room roomWithoutStats = new Room();
            roomWithoutStats.setId(UUID.randomUUID());
            roomWithoutStats.setRoomNumber("102");

            department.setRooms(List.of(room, roomWithoutStats));

            stubDepartments();
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(22f, 50f, 1000f));
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomWithoutStats.getId(), Granularity.DAILY))
                    .thenReturn(null);
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(null);

            BuildingTrend saved = capturedTrend();

            // raw = 0.4625 → value = (1 - 0.4625) * 100 = 53.75
            assertEquals(53.75, saved.getValue(), 0.0001);
        }

        @Test
        @DisplayName("no departments → no trends saved")
        void noDepartments_noSave() {
            when(departmentRepository.findAllWithRooms()).thenReturn(List.of());

            trendJob.trendDaily();

            verify(trendRepository, never()).save(any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // trendDaily — multiple departments
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("multiple departments")
    class MultipleDepartments {

        @Test
        @DisplayName("saves one BuildingTrend per department")
        void savesOneTrendPerDepartment() {
            UUID dept2Id   = UUID.randomUUID();
            UUID room2Id   = UUID.randomUUID();

            Room room2 = new Room();
            room2.setId(room2Id);
            room2.setRoomNumber("201");

            Department dept2 = new Department();
            dept2.setId(dept2Id);
            dept2.setName("Engineering");
            dept2.setRooms(List.of(room2));

            when(departmentRepository.findAllWithRooms()).thenReturn(List.of(department, dept2));

            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId,  Granularity.DAILY)).thenReturn(statsOf(22f, 50f, 1000f));
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(room2Id, Granularity.DAILY)).thenReturn(statsOf(20f, 40f, 800f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId)).thenReturn(null);
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(dept2Id)).thenReturn(null);

            trendJob.trendDaily();

            verify(trendRepository, times(2)).save(any(BuildingTrend.class));
        }
    }
}