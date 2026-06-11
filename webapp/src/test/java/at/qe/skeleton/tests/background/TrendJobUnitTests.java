package at.qe.skeleton.tests.background;

import at.qe.skeleton.background.TrendJob;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.AggregatedStatsRepository;
import at.qe.skeleton.repositories.BuildingTrendRepository;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.repositories.FormulaWeightsRepository;
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
import java.util.Collections;
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
    @Mock private FormulaWeightsRepository   formulaWeightsRepository;

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

    /** Stubs the weights repository to return default fallback (empty list). */
    private void stubDefaultWeights() {
        when(formulaWeightsRepository.findAll()).thenReturn(Collections.emptyList());
    }

    /** Stubs the weights repository with explicit weights. */
    private void stubWeights(double temp, double hum, double co2) {
        FormulaWeights w = FormulaWeights.builder()
                .tempWeight(temp)
                .humWeight(hum)
                .co2Weight(co2)
                .build();
        when(formulaWeightsRepository.findAll()).thenReturn(List.of(w));
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
            stubDefaultWeights();
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
            stubDefaultWeights();
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
            stubDefaultWeights();
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
            stubDefaultWeights();
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
            stubDefaultWeights();
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
            stubDefaultWeights();

            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId,  Granularity.DAILY)).thenReturn(statsOf(22f, 50f, 1000f));
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(room2Id, Granularity.DAILY)).thenReturn(statsOf(20f, 40f, 800f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId)).thenReturn(null);
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(dept2Id)).thenReturn(null);

            trendJob.trendDaily();

            verify(trendRepository, times(2)).save(any(BuildingTrend.class));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // avgFormula — custom FormulaWeights
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("custom formula weights")
    class CustomFormulaWeights {

        @Test
        @DisplayName("uses custom weights from repository when present")
        void usesCustomWeightsWhenPresent() {
            // weights: temp=0.5, hum=0.3, co2=0.2
            // normalizedTemp     = (22 - 18) / 8  = 0.5
            // normalizedHumidity = (50 - 30) / 40 = 0.5
            // normalizedCo2      = (1000 - 400) / 1600 = 0.375
            // raw = 0.5 * 0.5 + 0.3 * 0.5 + 0.2 * 0.375 = 0.25 + 0.15 + 0.075 = 0.475
            // value = (1 - 0.475) * 100 = 52.5
            double expectedValue = 52.5;

            stubDepartments();
            stubWeights(0.5, 0.3, 0.2);
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(22f, 50f, 1000f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(null);

            assertEquals(expectedValue, capturedTrend().getValue(), 0.0001);
        }

        @Test
        @DisplayName("falls back to default weights (0.4/0.3/0.3) when repository returns empty list")
        void fallsBackToDefaultWeightsWhenEmpty() {
            // default weights: temp=0.4, hum=0.3, co2=0.3
            // same sensor values as above → raw = 0.4625 → value = 53.75
            double expectedValue = 53.75;

            stubDepartments();
            stubDefaultWeights();
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(22f, 50f, 1000f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(null);

            assertEquals(expectedValue, capturedTrend().getValue(), 0.0001);
        }

        @Test
        @DisplayName("custom weights produce a different value than default weights for the same sensors")
        void customWeightsProduceDifferentValueThanDefaults() {
            // Confirm that custom weights (0.5/0.3/0.2) yield a different result
            // than the default (0.4/0.3/0.3) for identical sensor readings.
            // custom  → 52.5  (see test above)
            // default → 53.75 (see test above)
            stubDepartments();
            stubWeights(0.5, 0.3, 0.2);
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(22f, 50f, 1000f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(null);

            double customResult = capturedTrend().getValue();

            // Reset captured invocations so we can call capturedTrend() again
            clearInvocations(trendRepository);
            stubDefaultWeights();
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(22f, 50f, 1000f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(null);

            double defaultResult = capturedTrend().getValue();

            assertEquals(52.5,  customResult,  0.0001);
            assertEquals(53.75, defaultResult, 0.0001);
        }

        @Test
        @DisplayName("only first weight row is used when repository returns multiple rows")
        void usesFirstWeightRowOnly() {
            // First row: temp=0.5, hum=0.3, co2=0.2 → value = 52.5
            // Second row would give a different result; must be ignored.
            FormulaWeights first  = FormulaWeights.builder().tempWeight(0.5).humWeight(0.3).co2Weight(0.2).build();
            FormulaWeights second = FormulaWeights.builder().tempWeight(0.1).humWeight(0.1).co2Weight(0.8).build();
            when(formulaWeightsRepository.findAll()).thenReturn(List.of(first, second));

            stubDepartments();
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(22f, 50f, 1000f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(null);

            assertEquals(52.5, capturedTrend().getValue(), 0.0001);
        }

        @Test
        @DisplayName("temp-only weight (1.0/0.0/0.0) ignores humidity and CO2")
        void tempOnlyWeightIgnoresOtherSensors() {
            // weights: temp=1.0, hum=0.0, co2=0.0
            // normalizedTemp = (22 - 18) / 8 = 0.5
            // raw = 1.0 * 0.5 = 0.5
            // value = (1 - 0.5) * 100 = 50.0
            stubDepartments();
            stubWeights(1.0, 0.0, 0.0);
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(22f, 70f, 2000f)); // extreme hum/co2 — should not matter
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(null);

            assertEquals(50.0, capturedTrend().getValue(), 0.0001);
        }

        @Test
        @DisplayName("value is clamped to 0 when raw > 1 (weights push beyond upper bound)")
        void valueClampsToZeroWhenRawExceedsOne() {
            // weights: temp=2.0, hum=0.0, co2=0.0
            // normalizedTemp = (26 - 18) / 8 = 1.0
            // raw = 2.0 * 1.0 = 2.0 → clamped to 1.0
            // value = (1 - 1.0) * 100 = 0.0
            stubDepartments();
            stubWeights(2.0, 0.0, 0.0);
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(26f, 30f, 400f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(null);

            assertEquals(0.0, capturedTrend().getValue(), 0.0001);
        }

        @Test
        @DisplayName("value is clamped to 100 when raw < 0 (negative weights)")
        void valueClampsTo100WhenRawBelowZero() {
            // weights: temp=-1.0, hum=0.0, co2=0.0
            // normalizedTemp = (26 - 18) / 8 = 1.0
            // raw = -1.0 * 1.0 = -1.0 → clamped to 0.0
            // value = (1 - 0.0) * 100 = 100.0
            stubDepartments();
            stubWeights(-1.0, 0.0, 0.0);
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(26f, 30f, 400f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(null);

            assertEquals(100.0, capturedTrend().getValue(), 0.0001);
        }

        @Test
        @DisplayName("custom weights affect trend direction compared to defaults")
        void customWeightsCanFlipTrendDirection() {
            // With default weights (0.4/0.3/0.3), sensors (22, 50, 1000) → 53.75
            // previousTrend = 60.0 → DOWN (60 > 53.75)
            //
            // With custom weights (0.1/0.1/0.8), co2 dominates:
            // normalizedCo2 = (1000-400)/1600 = 0.375
            // raw = 0.1*0.5 + 0.1*0.5 + 0.8*0.375 = 0.05+0.05+0.3 = 0.4
            // value = (1-0.4)*100 = 60.0 → equal to previous → STABLE
            stubDepartments();
            stubWeights(0.1, 0.1, 0.8);
            when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.DAILY))
                    .thenReturn(statsOf(22f, 50f, 1000f));
            when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId))
                    .thenReturn(previousTrendWithValue(60.0));

            assertEquals(Trend.STABLE, capturedTrend().getTrend());
        }
    }
}