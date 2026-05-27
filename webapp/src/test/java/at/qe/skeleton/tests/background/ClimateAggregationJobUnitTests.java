package at.qe.skeleton.tests.background;

import at.qe.skeleton.background.ClimateAggregationJob;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClimateAggregationJobUnitTests {

    @Mock private ClimateStatsRepository climateStatsRepository;
    @Mock private AggregatedStatsRepository aggregatedStatsRepository;
    @Mock private RoomMonitoringRepository roomMonitoringRepository;
    @Mock private AggregatedDepartmentStatsRepository departmentStatsRepository;
    @Mock private DepartmentRepository departmentRepository;

    @InjectMocks
    private ClimateAggregationJob job;

    private UUID roomId;
    private RoomMonitoring roomMonitoring;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        roomMonitoring = mock(RoomMonitoring.class);
    }

    private void stubRoomMonitoring() {
        when(roomMonitoring.getRoomId()).thenReturn(roomId);
    }

    // -------------------------------------------------------------------------
    // aggregateDaily — room-level
    // -------------------------------------------------------------------------

    @Test
    void testAggregateDailySkipsRoomWhenAggregationAlreadyExists() {
        stubRoomMonitoring();
        when(roomMonitoringRepository.findAll()).thenReturn(List.of(roomMonitoring));
        when(departmentRepository.findAllWithRooms()).thenReturn(List.of());
        when(aggregatedStatsRepository.existsByRoomIdAndDateAndGranularity(
                eq(roomId), any(LocalDate.class), eq(Granularity.DAILY))).thenReturn(true);

        job.aggregateDaily();

        verify(climateStatsRepository, never()).findByRoomMonitoring_RoomIdAndDateBetween(any(), any(), any());
        verify(aggregatedStatsRepository, never()).save(any());
    }

    @Test
    void testAggregateDailySkipsRoomWhenNoClimateRecordsFound() {
        stubRoomMonitoring();
        when(roomMonitoringRepository.findAll()).thenReturn(List.of(roomMonitoring));
        when(departmentRepository.findAllWithRooms()).thenReturn(List.of());
        when(aggregatedStatsRepository.existsByRoomIdAndDateAndGranularity(any(), any(), any())).thenReturn(false);
        when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(
                eq(roomId), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(List.of());

        job.aggregateDaily();

        verify(aggregatedStatsRepository, never()).save(any());
    }

    @Test
    void testAggregateDailySavesCorrectAveragesForRoom() {
        stubRoomMonitoring();
        ClimateStats r1 = mockClimateStats(20.0f, 40.0f, 800.0f);
        ClimateStats r2 = mockClimateStats(22.0f, 60.0f, 1000.0f);

        when(roomMonitoringRepository.findAll()).thenReturn(List.of(roomMonitoring));
        when(departmentRepository.findAllWithRooms()).thenReturn(List.of());
        when(aggregatedStatsRepository.existsByRoomIdAndDateAndGranularity(any(), any(), any())).thenReturn(false);
        when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(
                eq(roomId), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(List.of(r1, r2));
        when(aggregatedStatsRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        job.aggregateDaily();

        ArgumentCaptor<AggregatedStats> captor = ArgumentCaptor.forClass(AggregatedStats.class);
        verify(aggregatedStatsRepository, times(1)).save(captor.capture());

        AggregatedStats saved = captor.getValue();
        assertEquals(roomId, saved.getRoomId());
        assertEquals(Granularity.DAILY, saved.getGranularity());
        assertEquals(21.0f, saved.getAvgTemp(), 0.01f);
        assertEquals(50.0f, saved.getAvgHumidity(), 0.01f);
        assertEquals(900.0f, saved.getAvgCO2(), 0.01f);
    }

    @Test
    void testAggregateDailySavesCorrectDateForRoom() {
        stubRoomMonitoring();
        ClimateStats r1 = mockClimateStats(20.0f, 40.0f, 800.0f);

        when(roomMonitoringRepository.findAll()).thenReturn(List.of(roomMonitoring));
        when(departmentRepository.findAllWithRooms()).thenReturn(List.of());
        when(aggregatedStatsRepository.existsByRoomIdAndDateAndGranularity(any(), any(), any())).thenReturn(false);
        when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(
                eq(roomId), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(List.of(r1));
        when(aggregatedStatsRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        job.aggregateDaily();

        ArgumentCaptor<AggregatedStats> captor = ArgumentCaptor.forClass(AggregatedStats.class);
        verify(aggregatedStatsRepository).save(captor.capture());
        assertEquals(LocalDate.now().minusDays(1), captor.getValue().getDate());
    }

    // -------------------------------------------------------------------------
    // aggregateWeekly
    // -------------------------------------------------------------------------

    @Test
    void testAggregateWeeklySkipsWhenWeeklyAlreadyExists() {
        stubRoomMonitoring();
        when(roomMonitoringRepository.findAll()).thenReturn(List.of(roomMonitoring));
        when(aggregatedStatsRepository.existsByRoomIdAndDateAndGranularity(
                eq(roomId), any(LocalDate.class), eq(Granularity.WEEKLY))).thenReturn(true);

        job.aggregateWeekly();

        verify(aggregatedStatsRepository, never()).findByRoomIdAndDateBetweenAndGranularity(any(), any(), any(), any());
        verify(aggregatedStatsRepository, never()).save(any());
    }

    @Test
    void testAggregateWeeklySkipsWhenNoDailyAggregationsFound() {
        stubRoomMonitoring();
        when(roomMonitoringRepository.findAll()).thenReturn(List.of(roomMonitoring));
        when(aggregatedStatsRepository.existsByRoomIdAndDateAndGranularity(any(), any(), eq(Granularity.WEEKLY))).thenReturn(false);
        when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
                eq(roomId), any(LocalDate.class), any(LocalDate.class), eq(Granularity.DAILY))).thenReturn(List.of());

        job.aggregateWeekly();

        verify(aggregatedStatsRepository, never()).save(any());
    }

    @Test
    void testAggregateWeeklySavesCorrectAveragesFromDailies() {
        stubRoomMonitoring();
        AggregatedStats d1 = mockAggregatedStats(18.0f, 35.0f, 700.0f);
        AggregatedStats d2 = mockAggregatedStats(22.0f, 45.0f, 900.0f);

        when(roomMonitoringRepository.findAll()).thenReturn(List.of(roomMonitoring));
        when(aggregatedStatsRepository.existsByRoomIdAndDateAndGranularity(any(), any(), eq(Granularity.WEEKLY))).thenReturn(false);
        when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
                eq(roomId), any(LocalDate.class), any(LocalDate.class), eq(Granularity.DAILY))).thenReturn(List.of(d1, d2));
        when(aggregatedStatsRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        job.aggregateWeekly();

        ArgumentCaptor<AggregatedStats> captor = ArgumentCaptor.forClass(AggregatedStats.class);
        verify(aggregatedStatsRepository).save(captor.capture());

        AggregatedStats saved = captor.getValue();
        assertEquals(roomId, saved.getRoomId());
        assertEquals(Granularity.WEEKLY, saved.getGranularity());
        assertEquals(20.0f, saved.getAvgTemp(), 0.01f);
        assertEquals(40.0f, saved.getAvgHumidity(), 0.01f);
        assertEquals(800.0f, saved.getAvgCO2(), 0.01f);
        assertEquals(LocalDate.now().minusDays(7), saved.getDate());
    }

    @Test
    void testAggregateWeeklyHandlesMultipleRooms() {
        stubRoomMonitoring();
        UUID roomId2 = UUID.randomUUID();
        RoomMonitoring rm2 = mock(RoomMonitoring.class);
        when(rm2.getRoomId()).thenReturn(roomId2);

        AggregatedStats daily = mockAggregatedStats(20.0f, 50.0f, 800.0f);

        when(roomMonitoringRepository.findAll()).thenReturn(List.of(roomMonitoring, rm2));
        when(aggregatedStatsRepository.existsByRoomIdAndDateAndGranularity(any(), any(), eq(Granularity.WEEKLY))).thenReturn(false);
        when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
                any(UUID.class), any(LocalDate.class), any(LocalDate.class), eq(Granularity.DAILY)))
                .thenReturn(List.of(daily));
        when(aggregatedStatsRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        job.aggregateWeekly();

        verify(aggregatedStatsRepository, times(2)).save(any(AggregatedStats.class));
    }

    // -------------------------------------------------------------------------
    // aggregateDaily — department-level
    // -------------------------------------------------------------------------

    @Test
    void testAggregateDailySkipsDepartmentWhenAggregationAlreadyExists() {
        Department department = Department.builder().id(UUID.randomUUID()).build();

        when(roomMonitoringRepository.findAll()).thenReturn(List.of());
        when(departmentRepository.findAllWithRooms()).thenReturn(List.of(department));
        when(departmentStatsRepository.existsByDepartmentIdAndDate(
                eq(department.getId()), any(LocalDate.class))).thenReturn(true);

        job.aggregateDaily();

        verify(departmentStatsRepository, never()).save(any());
    }

    @Test
    void testAggregateDailySkipsDepartmentRoomWhenNoRoomStatsFound() {
        Room room = mockRoomWithNumber();
        Department department = mockDepartmentWithName(List.of(room));

        when(roomMonitoringRepository.findAll()).thenReturn(List.of());
        when(departmentRepository.findAllWithRooms()).thenReturn(List.of(department));
        when(departmentStatsRepository.existsByDepartmentIdAndDate(any(), any())).thenReturn(false);
        when(aggregatedStatsRepository.findFirstByRoomIdAndDateAndGranularity(
                eq(room.getId()), any(LocalDate.class), eq(Granularity.DAILY))).thenReturn(Optional.empty());
        when(departmentStatsRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        job.aggregateDaily();

        // Still saves — but with zeroed-out averages since no room contributed
        ArgumentCaptor<AggregatedDepartmentStats> captor = ArgumentCaptor.forClass(AggregatedDepartmentStats.class);
        verify(departmentStatsRepository).save(captor.capture());
        // Division by rooms.size() (1) of accumulated 0 → all zeros
        assertEquals(0.0f, captor.getValue().getAvgCO2(), 0.01f);
        assertEquals(0.0f, captor.getValue().getAvgTemp(), 0.01f);
        assertEquals(0.0f, captor.getValue().getAvgHumidity(), 0.01f);
    }

    @Test
    void testAggregateDailySavesCorrectDepartmentAverages() {
        Room room1 = mockRoom();
        Room room2 = mockRoom();
        Department department = mockDepartmentWithName(List.of(room1, room2));

        AggregatedStats stats1 = mockAggregatedStats(20.0f, 40.0f, 800.0f);
        AggregatedStats stats2 = mockAggregatedStats(22.0f, 60.0f, 1000.0f);

        when(roomMonitoringRepository.findAll()).thenReturn(List.of());
        when(departmentRepository.findAllWithRooms()).thenReturn(List.of(department));
        when(departmentStatsRepository.existsByDepartmentIdAndDate(any(), any())).thenReturn(false);
        when(aggregatedStatsRepository.findFirstByRoomIdAndDateAndGranularity(
                eq(room1.getId()), any(LocalDate.class), eq(Granularity.DAILY))).thenReturn(Optional.of(stats1));
        when(aggregatedStatsRepository.findFirstByRoomIdAndDateAndGranularity(
                eq(room2.getId()), any(LocalDate.class), eq(Granularity.DAILY))).thenReturn(Optional.of(stats2));
        when(departmentStatsRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        job.aggregateDaily();

        ArgumentCaptor<AggregatedDepartmentStats> captor = ArgumentCaptor.forClass(AggregatedDepartmentStats.class);
        verify(departmentStatsRepository).save(captor.capture());

        AggregatedDepartmentStats saved = captor.getValue();
        assertEquals(department.getId(), saved.getDepartmentId());
        assertEquals(LocalDate.now().minusDays(1), saved.getDate());
        assertEquals(21.0f, saved.getAvgTemp(), 0.01f);
        assertEquals(50.0f, saved.getAvgHumidity(), 0.01f);
        assertEquals(900.0f, saved.getAvgCO2(), 0.01f);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ClimateStats mockClimateStats(float temp, float hum, float co2) {
        ClimateStats s = mock(ClimateStats.class);
        when(s.getTempVal()).thenReturn(Double.valueOf(temp));
        when(s.getHumVal()).thenReturn(Double.valueOf(hum));
        when(s.getPollVal()).thenReturn(Double.valueOf(co2));
        return s;
    }

    private AggregatedStats mockAggregatedStats(float temp, float hum, float co2) {
        AggregatedStats s = mock(AggregatedStats.class);
        when(s.getAvgTemp()).thenReturn(temp);
        when(s.getAvgHumidity()).thenReturn(hum);
        when(s.getAvgCO2()).thenReturn(co2);
        return s;
    }

    private Room mockRoom() {
        Room r = mock(Room.class);
        when(r.getId()).thenReturn(UUID.randomUUID());
        return r;
    }

    // Use when getRoomNumber() will actually be called (e.g. in log paths reached by the job)
    private Room mockRoomWithNumber() {
        Room r = mock(Room.class);
        when(r.getId()).thenReturn(UUID.randomUUID());
        when(r.getRoomNumber()).thenReturn("R-" + UUID.randomUUID().toString().substring(0, 4));
        return r;
    }

    private Department mockDepartment(List<Room> rooms) {
        Department d = mock(Department.class);
        when(d.getId()).thenReturn(UUID.randomUUID());
        when(d.getRooms()).thenReturn(rooms);
        return d;
    }

    // Use when getName() will actually be called (aggregation runs, hits the log line)
    private Department mockDepartmentWithName(List<Room> rooms) {
        Department d = mock(Department.class);
        when(d.getId()).thenReturn(UUID.randomUUID());
        when(d.getName()).thenReturn("Test Department");
        when(d.getRooms()).thenReturn(rooms);
        return d;
    }
}