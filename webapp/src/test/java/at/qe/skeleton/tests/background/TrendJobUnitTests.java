package at.qe.skeleton.tests.background;

import at.qe.skeleton.background.TrendJob;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.AggregatedStatsRepository;
import at.qe.skeleton.repositories.BuildingTrendRepository;
import at.qe.skeleton.repositories.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendJobUnitTests {

    @Mock private BuildingTrendRepository trendRepository;
    @Mock private AggregatedStatsRepository aggregatedStatsRepository;
    @Mock private DepartmentRepository departmentRepository;

    @InjectMocks private TrendJob trendJob;

    private Department department;
    private Room room;
    private UUID deptId;
    private UUID roomId;

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

    @Test
    void testThatTrendWeeklyCalculatesUpTrendCorrectly() {
        // Arrange
        // Formula calculation input: temp=22, hum=50, co2=1000
        // normTemp = (22-18)/(26-18) = 0.5 -> weight 0.4 * 0.5 = 0.2
        // normHum = (50-30)/(70-30) = 0.5 -> weight 0.3 * 0.5 = 0.15
        // normCo2 = (1000-400)/(2000-400) = 0.375 -> weight 0.3 * 0.375 = 0.1125
        // Expected value score = 0.2 + 0.15 + 0.1125 = 0.4625
        double calculatedValue = 0.4625;

        AggregatedStats stats = AggregatedStats.builder()
                .avgTemp(22.0f)
                .avgHumidity(50.0f)
                .avgCO2(1000.0f)
                .build();

        BuildingTrend lastTrend = BuildingTrend.builder()
                .value(0.3000) // Lower than calculated value -> Trend should go UP
                .build();

        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.WEEKLY))
                .thenReturn(stats);
        when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId)).thenReturn(lastTrend);

        // Act
        trendJob.trendWeekly();

        // Assert
        ArgumentCaptor<BuildingTrend> trendCaptor = ArgumentCaptor.forClass(BuildingTrend.class);
        verify(trendRepository).save(trendCaptor.capture());

        BuildingTrend savedTrend = trendCaptor.getValue();
        assertEquals(deptId, savedTrend.getDepartmentId());
        assertEquals("Informatics", savedTrend.getDepartmentName());
        assertEquals(calculatedValue, savedTrend.getValue(), 0.0001);
        assertEquals(Trend.UP, savedTrend.getTrend());
    }

    @Test
    void testThatTrendWeeklyCalculatesDownTrendCorrectly() {
        // Arrange
        AggregatedStats stats = AggregatedStats.builder()
                .avgTemp(18.0f) // normTemp = 0
                .avgHumidity(30.0f) // normHum = 0
                .avgCO2(400.0f) // normCo2 = 0
                .build(); // value score = 0.0

        BuildingTrend lastTrend = BuildingTrend.builder()
                .value(0.1500) // Higher than calculated value (0.0) -> Trend should go DOWN
                .build();

        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.WEEKLY))
                .thenReturn(stats);
        when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId)).thenReturn(lastTrend);

        // Act
        trendJob.trendWeekly();

        // Assert
        ArgumentCaptor<BuildingTrend> trendCaptor = ArgumentCaptor.forClass(BuildingTrend.class);
        verify(trendRepository).save(trendCaptor.capture());
        assertEquals(Trend.DOWN, trendCaptor.getValue().getTrend());
    }

    @Test
    void testThatTrendWeeklySetsStableTrendWhenValuesAreSame() {
        // Arrange
        AggregatedStats stats = AggregatedStats.builder()
                .avgTemp(26.0f) // normTemp = 1.0 -> 0.4
                .avgHumidity(70.0f) // normHum = 1.0 -> 0.3
                .avgCO2(2000.0f) // normCo2 = 1.0 -> 0.3
                .build(); // value score = 1.0

        BuildingTrend lastTrend = BuildingTrend.builder()
                .value(1.0) // Exactly matches the calculated value -> Trend should be STABLE
                .build();

        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.WEEKLY))
                .thenReturn(stats);
        when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId)).thenReturn(lastTrend);

        // Act
        trendJob.trendWeekly();

        // Assert
        ArgumentCaptor<BuildingTrend> trendCaptor = ArgumentCaptor.forClass(BuildingTrend.class);
        verify(trendRepository).save(trendCaptor.capture());
        assertEquals(Trend.STABLE, trendCaptor.getValue().getTrend());
    }

    @Test
    void testThatTrendWeeklyDefaultsToStableWhenNoPreviousTrendExists() {
        // Arrange
        AggregatedStats stats = AggregatedStats.builder()
                .avgTemp(22.0f).avgHumidity(50.0f).avgCO2(1000.0f).build();

        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.WEEKLY))
                .thenReturn(stats);
        when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId)).thenReturn(null); // No history

        // Act
        trendJob.trendWeekly();

        // Assert
        ArgumentCaptor<BuildingTrend> trendCaptor = ArgumentCaptor.forClass(BuildingTrend.class);
        verify(trendRepository).save(trendCaptor.capture());
        assertEquals(Trend.STABLE, trendCaptor.getValue().getTrend());
    }

    @Test
    void testThatTrendWeeklySkipsRoomWhenStatsAreMissing() {
        // Arrange
        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(roomId, Granularity.WEEKLY))
                .thenReturn(null);
        when(trendRepository.findFirstByDepartmentIdOrderByDateDesc(deptId)).thenReturn(null);

        // Act
        trendJob.trendWeekly();

        // Assert
        ArgumentCaptor<BuildingTrend> trendCaptor = ArgumentCaptor.forClass(BuildingTrend.class);
        verify(trendRepository).save(trendCaptor.capture());

        assertEquals(0.0, trendCaptor.getValue().getValue());
    }
}