package at.qe.skeleton.tests.services;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.AggregatedStatsMapper;
import at.qe.skeleton.mappers.ClimateDataPointMapper;
import at.qe.skeleton.mappers.LimitMapper;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.AggregatedStatsRepository;
import at.qe.skeleton.repositories.ClimateStatsRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.impl.ClimateStatsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClimateStatsServiceUnitTests {

    @Mock private ClimateStatsRepository climateStatsRepository;
    @Mock private AggregatedStatsRepository aggregatedStatsRepository;
    @Mock private RoomMonitoringRepository roomMonitoringRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private ClimateDataPointMapper climateMapper;
    @Mock private AggregatedStatsMapper aggregatedMapper;
    @Mock private LimitMapper limitMapper;
    @Mock private AuthenticatedUserService authenticatedUserService;
    @Mock private Userx user;

    @InjectMocks
    private ClimateStatsServiceImpl climateStatsService;

    // Shared fixtures
    private UUID roomId;
    private Room officeRoom;
    private Room sharedRoom;
    private Department department;
    private Department otherDepartment;
    private Room userRoom;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();

        department = new Department();
        department.setId(UUID.randomUUID());

        otherDepartment = new Department();
        otherDepartment.setId(UUID.randomUUID());

        officeRoom = new Room();
        officeRoom.setId(roomId);
        officeRoom.setDepartment(department);
        officeRoom.setRoomType(RoomType.OFFICE);

        sharedRoom = new Room();
        sharedRoom.setId(UUID.randomUUID());
        sharedRoom.setDepartment(department);
        sharedRoom.setRoomType(RoomType.SHARED);

        userRoom = new Room();
        userRoom.setId(roomId);
        userRoom.setDepartment(department);
        userRoom.setRoomType(RoomType.OFFICE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getCurrentClimate
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    class GetCurrentClimate {

        @Test
        void buildingManager_canViewAnyRoom() {
            ClimateStats stats = ClimateStats.builder().build();
            ClimateDataPointDTO dto = new ClimateDataPointDTO(OffsetDateTime.now(), 22.0, 50.0, 400.0);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_ALL_ROOMS")));
            when(climateStatsRepository.findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId))
                    .thenReturn(Optional.of(stats));
            when(climateMapper.mapTo(stats)).thenReturn(dto);

            ClimateDataPointDTO result = climateStatsService.getCurrentClimate(roomId);

            assertNotNull(result);
            assertEquals(dto, result);
        }

        @Test
        void buildingManager_throwsNotFound_whenNoData() {
            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_ALL_ROOMS")));
            when(climateStatsRepository.findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> climateStatsService.getCurrentClimate(roomId));
        }

        @Test
        void deptHead_canViewOwnDepartmentRoom() {
            ClimateStats stats = ClimateStats.builder().build();
            ClimateDataPointDTO dto = new ClimateDataPointDTO(OffsetDateTime.now(), 21.0, 48.0, 390.0);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_DEPARTMENT_MEASURES")));
            when(user.getMyRoom()).thenReturn(userRoom); // same department
            when(climateStatsRepository.findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId))
                    .thenReturn(Optional.of(stats));
            when(climateMapper.mapTo(stats)).thenReturn(dto);

            ClimateDataPointDTO result = climateStatsService.getCurrentClimate(roomId);

            assertEquals(dto, result);
        }

        @Test
        void deptHead_throwsForbidden_whenDifferentDepartment() {
            Room foreignRoom = new Room();
            foreignRoom.setId(UUID.randomUUID());
            foreignRoom.setDepartment(otherDepartment);
            foreignRoom.setRoomType(RoomType.OFFICE);

            Room myRoom = new Room();
            myRoom.setId(UUID.randomUUID());
            myRoom.setDepartment(department);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(foreignRoom.getId())).thenReturn(Optional.of(foreignRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_DEPARTMENT_MEASURES")));
            when(user.getMyRoom()).thenReturn(myRoom);

            assertThrows(ForbiddenException.class,
                    () -> climateStatsService.getCurrentClimate(foreignRoom.getId()));
        }

        @Test
        void deptHead_throwsForbidden_whenMyRoomIsNull() {
            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_DEPARTMENT_MEASURES")));
            when(user.getMyRoom()).thenReturn(null);

            assertThrows(ForbiddenException.class,
                    () -> climateStatsService.getCurrentClimate(roomId));
        }

        @Test
        void employee_canViewOwnOffice() {
            ClimateStats stats = ClimateStats.builder().build();
            ClimateDataPointDTO dto = new ClimateDataPointDTO(OffsetDateTime.now(), 20.0, 55.0, 410.0);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_OFFICE_CLIMATE")));
            when(user.getMyRoom()).thenReturn(userRoom); // same room id
            when(climateStatsRepository.findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId))
                    .thenReturn(Optional.of(stats));
            when(climateMapper.mapTo(stats)).thenReturn(dto);

            ClimateDataPointDTO result = climateStatsService.getCurrentClimate(roomId);

            assertEquals(dto, result);
        }

        @Test
        void employee_canViewSharedRoomInOwnDepartment() {
            UUID sharedId = sharedRoom.getId();
            ClimateStats stats = ClimateStats.builder().build();
            ClimateDataPointDTO dto = new ClimateDataPointDTO(OffsetDateTime.now(), 20.0, 55.0, 410.0);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(sharedId)).thenReturn(Optional.of(sharedRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_OFFICE_CLIMATE")));
            when(user.getMyRoom()).thenReturn(userRoom); // same department, different room
            when(climateStatsRepository.findTopByRoomMonitoring_RoomIdOrderByDateDesc(sharedId))
                    .thenReturn(Optional.of(stats));
            when(climateMapper.mapTo(stats)).thenReturn(dto);

            ClimateDataPointDTO result = climateStatsService.getCurrentClimate(sharedId);

            assertEquals(dto, result);
        }

        @Test
        void employee_throwsForbidden_whenRequestingOtherOffice() {
            UUID otherRoomId = UUID.randomUUID();
            Room otherRoom = new Room();
            otherRoom.setId(otherRoomId);
            otherRoom.setRoomType(RoomType.OFFICE);
            otherRoom.setDepartment(department);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(otherRoomId)).thenReturn(Optional.of(otherRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_OFFICE_CLIMATE")));
            when(user.getMyRoom()).thenReturn(userRoom); // different room id

            assertThrows(ForbiddenException.class,
                    () -> climateStatsService.getCurrentClimate(otherRoomId));
        }

        @Test
        void employee_throwsForbidden_whenRequestingSharedRoomInOtherDepartment() {
            Room foreignShared = new Room();
            foreignShared.setId(UUID.randomUUID());
            foreignShared.setRoomType(RoomType.SHARED);
            foreignShared.setDepartment(otherDepartment);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(foreignShared.getId())).thenReturn(Optional.of(foreignShared));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_OFFICE_CLIMATE")));
            when(user.getMyRoom()).thenReturn(userRoom);

            assertThrows(ForbiddenException.class,
                    () -> climateStatsService.getCurrentClimate(foreignShared.getId()));
        }

        @Test
        void noMatchingRole_throwsForbidden() {
            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn((Collection) List.of());

            assertThrows(ForbiddenException.class,
                    () -> climateStatsService.getCurrentClimate(roomId));
        }

        @Test
        void throwsNotFound_whenRoomDoesNotExist() {
            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> climateStatsService.getCurrentClimate(roomId));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // saveMeasurementBatch
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    class SaveMeasurementBatch {

        @Test
        void persistsAllThreeMeasurementTypes() {
            UUID monitoringId = UUID.randomUUID();
            RoomMonitoring monitoring = new RoomMonitoring();
            monitoring.setRoomId(monitoringId);

            MeasurementBatchDTO batch = new MeasurementBatchDTO(
                    monitoringId,
                    OffsetDateTime.now(),
                    List.of(
                            new ReadingDTO(MeasurementType.TEMPERATURE, 23.5),
                            new ReadingDTO(MeasurementType.HUMIDITY, 55.0),
                            new ReadingDTO(MeasurementType.CO2, 420.0)
                    )
            );

            when(roomMonitoringRepository.findById(monitoringId)).thenReturn(Optional.of(monitoring));

            climateStatsService.saveMeasurementBatch(batch);

            verify(climateStatsRepository).save(argThat(stats ->
                    stats.getTempVal() == 23.5 &&
                            stats.getHumVal() == 55.0 &&
                            stats.getPollVal() == 420.0 &&
                            stats.getRoomMonitoring().equals(monitoring)
            ));
        }

        @Test
        void throwsNotFound_whenRoomMonitoringMissing() {
            UUID monitoringId = UUID.randomUUID();
            MeasurementBatchDTO batch = new MeasurementBatchDTO(
                    monitoringId,
                    OffsetDateTime.now(),
                    List.of(new ReadingDTO(MeasurementType.TEMPERATURE, 20.0))
            );

            when(roomMonitoringRepository.findById(monitoringId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> climateStatsService.saveMeasurementBatch(batch));
            verify(climateStatsRepository, never()).save(any());
        }

        @Test
        void handlesPartialReadings_missingCO2() {
            UUID monitoringId = UUID.randomUUID();
            RoomMonitoring monitoring = new RoomMonitoring();
            monitoring.setRoomId(monitoringId);

            MeasurementBatchDTO batch = new MeasurementBatchDTO(
                    monitoringId,
                    OffsetDateTime.now(),
                    List.of(
                            new ReadingDTO(MeasurementType.TEMPERATURE, 21.0),
                            new ReadingDTO(MeasurementType.HUMIDITY, 60.0)
                    )
            );

            when(roomMonitoringRepository.findById(monitoringId)).thenReturn(Optional.of(monitoring));

            climateStatsService.saveMeasurementBatch(batch);

            // CO2 defaults to 0 since no reading was provided
            verify(climateStatsRepository).save(argThat(stats ->
                    stats.getTempVal() == 21.0 &&
                            stats.getHumVal() == 60.0 &&
                            stats.getPollVal() == 0.0
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getOvertime
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    class GetOvertime {

        private final LocalDate start = LocalDate.now().minusDays(1);
        private final LocalDate end   = LocalDate.now();

        @Test
        void buildingManager_returnsData() {
            ClimateStats stats = ClimateStats.builder().build();
            ClimateDataPointDTO dto = new ClimateDataPointDTO(OffsetDateTime.now(), 21.0, 45.0, 350.0);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_ALL_ROOMS")));
            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(eq(roomId), any(), any()))
                    .thenReturn(List.of(stats));
            when(climateMapper.mapTo(stats)).thenReturn(dto);

            List<ClimateDataPointDTO> result = climateStatsService.getOvertime(
                    roomId, start, end, LocalTime.MIN, LocalTime.MAX);

            assertEquals(1, result.size());
            assertEquals(dto, result.get(0));
        }

        @Test
        void deptHead_canViewOwnDepartmentOffice() {
            ClimateStats stats = ClimateStats.builder().build();
            ClimateDataPointDTO dto = new ClimateDataPointDTO(OffsetDateTime.now(), 19.0, 43.0, 330.0);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_DEPARTMENT_MEASURES")));
            when(user.getMyRoom()).thenReturn(userRoom);
            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(eq(roomId), any(), any()))
                    .thenReturn(List.of(stats));
            when(climateMapper.mapTo(stats)).thenReturn(dto);

            List<ClimateDataPointDTO> result = climateStatsService.getOvertime(
                    roomId, start, end, null, null);

            assertEquals(1, result.size());
        }

        @Test
        void deptHead_throwsForbidden_whenDifferentDepartment() {
            Room foreignRoom = new Room();
            foreignRoom.setId(UUID.randomUUID());
            foreignRoom.setDepartment(otherDepartment);
            foreignRoom.setRoomType(RoomType.OFFICE);

            Room myRoom = new Room();
            myRoom.setId(UUID.randomUUID());
            myRoom.setDepartment(department);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(foreignRoom.getId())).thenReturn(Optional.of(foreignRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_DEPARTMENT_MEASURES")));
            when(user.getMyRoom()).thenReturn(myRoom);

            assertThrows(ForbiddenException.class, () ->
                    climateStatsService.getOvertime(foreignRoom.getId(), start, end, null, null));
        }

        @Test
        void employee_canViewOwnOfficeOvertime() {
            ClimateStats stats = ClimateStats.builder().build();
            ClimateDataPointDTO dto = new ClimateDataPointDTO(OffsetDateTime.now(), 20.0, 50.0, 400.0);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_OFFICE_CLIMATE")));
            when(user.getMyRoom()).thenReturn(userRoom);
            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(eq(roomId), any(), any()))
                    .thenReturn(List.of(stats));
            when(climateMapper.mapTo(stats)).thenReturn(dto);

            List<ClimateDataPointDTO> result = climateStatsService.getOvertime(
                    roomId, start, end, null, null);

            assertEquals(1, result.size());
        }

        @Test
        void employee_canViewSharedRoomInOwnDepartment() {
            UUID sharedId = sharedRoom.getId();
            ClimateStats stats = ClimateStats.builder().build();
            ClimateDataPointDTO dto = new ClimateDataPointDTO(OffsetDateTime.now(), 20.0, 50.0, 400.0);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(sharedId)).thenReturn(Optional.of(sharedRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_OFFICE_CLIMATE")));
            when(user.getMyRoom()).thenReturn(userRoom);
            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(eq(sharedId), any(), any()))
                    .thenReturn(List.of(stats));
            when(climateMapper.mapTo(stats)).thenReturn(dto);

            List<ClimateDataPointDTO> result = climateStatsService.getOvertime(
                    sharedId, start, end, null, null);

            assertEquals(1, result.size());
        }

        @Test
        void employee_throwsForbidden_whenRequestingOtherOffice() {
            UUID otherRoomId = UUID.randomUUID();
            Room otherRoom = new Room();
            otherRoom.setId(otherRoomId);
            otherRoom.setRoomType(RoomType.OFFICE);
            otherRoom.setDepartment(department);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(otherRoomId)).thenReturn(Optional.of(otherRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_OFFICE_CLIMATE")));
            when(user.getMyRoom()).thenReturn(userRoom);

            assertThrows(ForbiddenException.class, () ->
                    climateStatsService.getOvertime(otherRoomId, start, end, null, null));
        }

        @Test
        void employee_throwsForbidden_whenSharedRoomDifferentDepartment() {
            Room foreignShared = new Room();
            foreignShared.setId(UUID.randomUUID());
            foreignShared.setRoomType(RoomType.SHARED);
            foreignShared.setDepartment(otherDepartment);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(foreignShared.getId())).thenReturn(Optional.of(foreignShared));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_OWN_OFFICE_CLIMATE")));
            when(user.getMyRoom()).thenReturn(userRoom);

            assertThrows(ForbiddenException.class, () ->
                    climateStatsService.getOvertime(foreignShared.getId(), start, end, null, null));
        }

        @Test
        void throwsValidation_whenEndBeforeStart() {
            assertThrows(ValidationException.class, () ->
                    climateStatsService.getOvertime(roomId, end, start, null, null));
        }

        @Test
        void throwsValidation_whenRangeExceedsTwoDays() {
            LocalDate farFuture = LocalDate.now().plusDays(5);
            assertThrows(ValidationException.class, () ->
                    climateStatsService.getOvertime(roomId, start, farFuture, null, null));
        }

        @Test
        void throwsNotFound_whenRoomDoesNotExist() {
            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () ->
                    climateStatsService.getOvertime(roomId, start, end, null, null));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getClimateHistoryFull
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    class GetClimateHistoryFull {

        @Test
        void buildingManager_returnsAggregatedDailyData() {
            AggregatedStats stats = new AggregatedStats();
            AggregatedDataPointDTO dto = new AggregatedDataPointDTO(LocalDate.now(), 22.0, 50.0, 400.0);

            LocalDate from = LocalDate.now().minusDays(5);
            LocalDate to   = LocalDate.now();

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_ALL_ROOMS")));
            when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
                    roomId, from, to, Granularity.DAILY)).thenReturn(List.of(stats));
            when(aggregatedMapper.mapTo(stats)).thenReturn(dto);

            List<AggregatedDataPointDTO> result =
                    climateStatsService.getClimateHistoryFull(roomId, from, to, "DAY");

            assertEquals(1, result.size());
            assertEquals(dto, result.get(0));
        }

        @Test
        void buildingManager_returnsAggregatedWeeklyData() {
            AggregatedStats stats = new AggregatedStats();
            AggregatedDataPointDTO dto = new AggregatedDataPointDTO(LocalDate.now(), 21.0, 48.0, 390.0);

            LocalDate from = LocalDate.now().minusDays(60);
            LocalDate to   = LocalDate.now();

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_ALL_ROOMS")));
            when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
                    roomId, from, to, Granularity.WEEKLY)).thenReturn(List.of(stats));
            when(aggregatedMapper.mapTo(stats)).thenReturn(dto);

            List<AggregatedDataPointDTO> result =
                    climateStatsService.getClimateHistoryFull(roomId, from, to, "WEEK");

            assertEquals(1, result.size());
        }

        @Test
        void buildingManager_hourGranularity_shortRange_groupsRawByHour() {
            OffsetDateTime ts = LocalDate.now().atTime(10, 0)
                    .atZone(ZoneId.systemDefault()).toOffsetDateTime();
            ClimateStats stats = ClimateStats.builder()
                    .date(ts).tempVal(20.0).humVal(50.0).pollVal(400.0).build();

            LocalDate from = LocalDate.now().minusDays(1);
            LocalDate to   = LocalDate.now();

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_ALL_ROOMS")));
            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(eq(roomId), any(), any()))
                    .thenReturn(List.of(stats));

            List<AggregatedDataPointDTO> result =
                    climateStatsService.getClimateHistoryFull(roomId, from, to, "HOUR");

            assertEquals(1, result.size());
            assertEquals(20.0, result.get(0).avgTemperature());
        }

        @Test
        void buildingManager_fallsBackToRawDay_whenAggregatedEmpty() {
            OffsetDateTime ts = LocalDate.now().minusDays(3).atStartOfDay()
                    .atZone(ZoneId.systemDefault()).toOffsetDateTime();
            ClimateStats stats = ClimateStats.builder()
                    .date(ts).tempVal(19.0).humVal(47.0).pollVal(380.0).build();

            LocalDate from = LocalDate.now().minusDays(5);
            LocalDate to   = LocalDate.now();

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_ALL_ROOMS")));
            when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
                    roomId, from, to, Granularity.DAILY)).thenReturn(List.of());
            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(eq(roomId), any(), any()))
                    .thenReturn(List.of(stats));

            List<AggregatedDataPointDTO> result =
                    climateStatsService.getClimateHistoryFull(roomId, from, to, "DAY");

            assertFalse(result.isEmpty());
            assertEquals(19.0, result.get(0).avgTemperature());
        }

        @Test
        void buildingManager_fallsBackToRawWeek_whenAggregatedEmpty() {
            OffsetDateTime ts = LocalDate.now().minusDays(50).atStartOfDay()
                    .atZone(ZoneId.systemDefault()).toOffsetDateTime();
            ClimateStats stats = ClimateStats.builder()
                    .date(ts).tempVal(18.0).humVal(46.0).pollVal(370.0).build();

            LocalDate from = LocalDate.now().minusDays(60);
            LocalDate to   = LocalDate.now();

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn(
                    (Collection) List.of(new SimpleGrantedAuthority("CAN_VIEW_ALL_ROOMS")));
            when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
                    roomId, from, to, Granularity.WEEKLY)).thenReturn(List.of());
            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(eq(roomId), any(), any()))
                    .thenReturn(List.of(stats));

            List<AggregatedDataPointDTO> result =
                    climateStatsService.getClimateHistoryFull(roomId, from, to, "WEEK");

            assertFalse(result.isEmpty());
        }

        @Test
        void employee_canViewOwnOfficeHistory() {
            AggregatedStats stats = new AggregatedStats();
            AggregatedDataPointDTO dto = new AggregatedDataPointDTO(LocalDate.now(), 22.0, 50.0, 400.0);

            LocalDate from = LocalDate.now().minusDays(5);
            LocalDate to   = LocalDate.now();

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getAuthorities()).thenReturn((Collection) List.of());
            when(user.getMyRoom()).thenReturn(userRoom);
            when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
                    roomId, from, to, Granularity.DAILY)).thenReturn(List.of(stats));
            when(aggregatedMapper.mapTo(stats)).thenReturn(dto);

            List<AggregatedDataPointDTO> result =
                    climateStatsService.getClimateHistoryFull(roomId, from, to, "DAY");

            assertEquals(1, result.size());
        }

        @Test
        void employee_throwsForbidden_whenAccessingOtherOffice() {
            UUID otherId = UUID.randomUUID();
            Room otherOffice = new Room();
            otherOffice.setId(otherId);
            otherOffice.setRoomType(RoomType.OFFICE);
            otherOffice.setDepartment(department);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(otherId)).thenReturn(Optional.of(otherOffice));
            when(user.getAuthorities()).thenReturn((Collection) List.of());
            when(user.getMyRoom()).thenReturn(userRoom);

            assertThrows(ForbiddenException.class, () ->
                    climateStatsService.getClimateHistoryFull(otherId,
                            LocalDate.now().minusDays(5), LocalDate.now(), "DAY"));
        }

        @Test
        void employee_throwsForbidden_whenAccessingSharedInOtherDept() {
            Room foreignShared = new Room();
            foreignShared.setId(UUID.randomUUID());
            foreignShared.setRoomType(RoomType.SHARED);
            foreignShared.setDepartment(otherDepartment);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(foreignShared.getId())).thenReturn(Optional.of(foreignShared));
            when(user.getAuthorities()).thenReturn((Collection) List.of());
            when(user.getMyRoom()).thenReturn(userRoom);

            assertThrows(ForbiddenException.class, () ->
                    climateStatsService.getClimateHistoryFull(foreignShared.getId(),
                            LocalDate.now().minusDays(5), LocalDate.now(), "DAY"));
        }

        @Test
        void throwsValidation_whenFromAfterTo() {
            assertThrows(ValidationException.class, () ->
                    climateStatsService.getClimateHistoryFull(roomId,
                            LocalDate.now(), LocalDate.now().minusDays(5), "DAY"));
        }

        @Test
        void throwsNotFound_whenRoomDoesNotExist() {
            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () ->
                    climateStatsService.getClimateHistoryFull(roomId,
                            LocalDate.now().minusDays(5), LocalDate.now(), "DAY"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getClimateHistoryReduced
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    class GetClimateHistoryReduced {

        @Test
        void deptHead_returnsDailyAggregated_forOwnDepartment() {
            AggregatedStats stats = new AggregatedStats();
            AggregatedDataPointDTO dto = new AggregatedDataPointDTO(LocalDate.now(), 22.0, 50.0, 400.0);

            LocalDate from = LocalDate.now().minusDays(10);
            LocalDate to   = LocalDate.now();

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getMyRoom()).thenReturn(userRoom);
            when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
                    roomId, from, to, Granularity.DAILY)).thenReturn(List.of(stats));
            when(aggregatedMapper.mapTo(stats)).thenReturn(dto);

            List<AggregatedDataPointDTO> result =
                    climateStatsService.getClimateHistoryReduced(roomId, from, to, "DAY");

            assertEquals(1, result.size());
            assertEquals(dto, result.get(0));
        }

        @Test
        void deptHead_returnsWeeklyAggregated_forLongRange() {
            AggregatedStats stats = new AggregatedStats();
            AggregatedDataPointDTO dto = new AggregatedDataPointDTO(LocalDate.now(), 21.0, 49.0, 395.0);

            LocalDate from = LocalDate.now().minusDays(60);
            LocalDate to   = LocalDate.now();

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getMyRoom()).thenReturn(userRoom);
            when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
                    roomId, from, to, Granularity.WEEKLY)).thenReturn(List.of(stats));
            when(aggregatedMapper.mapTo(stats)).thenReturn(dto);

            List<AggregatedDataPointDTO> result =
                    climateStatsService.getClimateHistoryReduced(roomId, from, to, "WEEK");

            assertEquals(1, result.size());
        }

        @Test
        void deptHead_canViewHourly_forOwnOffice() {
            OffsetDateTime ts = LocalDate.now().atTime(9, 0)
                    .atZone(ZoneId.systemDefault()).toOffsetDateTime();
            ClimateStats stats = ClimateStats.builder()
                    .date(ts).tempVal(22.0).humVal(51.0).pollVal(405.0).build();

            LocalDate from = LocalDate.now().minusDays(1);
            LocalDate to   = LocalDate.now();

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getMyRoom()).thenReturn(userRoom); // same room → hourly allowed
            when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
                    roomId, from, to, Granularity.DAILY)).thenReturn(List.of());
            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(eq(roomId), any(), any()))
                    .thenReturn(List.of(stats));

            List<AggregatedDataPointDTO> result =
                    climateStatsService.getClimateHistoryReduced(roomId, from, to, "HOUR");

            assertFalse(result.isEmpty());
        }

        @Test
        void deptHead_throwsForbidden_whenHourly_forOtherOfficeInDept() {
            UUID otherId = UUID.randomUUID();
            Room otherOffice = new Room();
            otherOffice.setId(otherId);
            otherOffice.setRoomType(RoomType.OFFICE);
            otherOffice.setDepartment(department);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(otherId)).thenReturn(Optional.of(otherOffice));
            when(user.getMyRoom()).thenReturn(userRoom); // same dept but different room

            assertThrows(ForbiddenException.class, () ->
                    climateStatsService.getClimateHistoryReduced(otherId,
                            LocalDate.now().minusDays(1), LocalDate.now(), "HOUR"));
        }

        @Test
        void deptHead_canViewHourly_forSharedRoomInDept() {
            UUID sharedId = sharedRoom.getId();
            OffsetDateTime ts = LocalDate.now().atTime(11, 0)
                    .atZone(ZoneId.systemDefault()).toOffsetDateTime();
            ClimateStats stats = ClimateStats.builder()
                    .date(ts).tempVal(21.0).humVal(50.0).pollVal(400.0).build();

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(sharedId)).thenReturn(Optional.of(sharedRoom));
            when(user.getMyRoom()).thenReturn(userRoom);
            when(aggregatedStatsRepository.findByRoomIdAndDateBetweenAndGranularity(
                    sharedId, LocalDate.now().minusDays(1), LocalDate.now(), Granularity.DAILY))
                    .thenReturn(List.of());
            when(climateStatsRepository.findByRoomMonitoring_RoomIdAndDateBetween(eq(sharedId), any(), any()))
                    .thenReturn(List.of(stats));

            List<AggregatedDataPointDTO> result =
                    climateStatsService.getClimateHistoryReduced(sharedId,
                            LocalDate.now().minusDays(1), LocalDate.now(), "HOUR");

            assertFalse(result.isEmpty());
        }

        @Test
        void throwsForbidden_whenDifferentDepartment() {
            Room foreignRoom = new Room();
            foreignRoom.setId(UUID.randomUUID());
            foreignRoom.setDepartment(otherDepartment);
            foreignRoom.setRoomType(RoomType.OFFICE);

            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(foreignRoom.getId())).thenReturn(Optional.of(foreignRoom));
            when(user.getMyRoom()).thenReturn(userRoom);

            assertThrows(ForbiddenException.class, () ->
                    climateStatsService.getClimateHistoryReduced(foreignRoom.getId(),
                            LocalDate.now().minusDays(10), LocalDate.now(), "DAY"));
        }

        @Test
        void throwsForbidden_whenMyRoomIsNull() {
            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(officeRoom));
            when(user.getMyRoom()).thenReturn(null);

            assertThrows(ForbiddenException.class, () ->
                    climateStatsService.getClimateHistoryReduced(roomId,
                            LocalDate.now().minusDays(10), LocalDate.now(), "DAY"));
        }

        @Test
        void throwsValidation_whenFromAfterTo() {

            assertThrows(ValidationException.class, () ->
                    climateStatsService.getClimateHistoryReduced(roomId,
                            LocalDate.now(), LocalDate.now().minusDays(10), "DAY"));
        }

        @Test
        void throwsNotFound_whenRoomDoesNotExist() {
            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
            when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () ->
                    climateStatsService.getClimateHistoryReduced(roomId,
                            LocalDate.now().minusDays(10), LocalDate.now(), "DAY"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getLimits
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    class GetLimits {

        @Test
        void returnsLimitDTO_whenMonitoringExists() {
            UUID monitoringId = UUID.randomUUID();
            RoomMonitoring monitoring = new RoomMonitoring();
            monitoring.setRoomId(monitoringId);
            LimitDTO dto = new LimitDTO(monitoringId, 25.f, 60.f, 1000.f, 2000.f, 200.f);

            when(roomMonitoringRepository.findById(monitoringId)).thenReturn(Optional.of(monitoring));
            when(limitMapper.mapTo(monitoring)).thenReturn(dto);

            LimitDTO result = climateStatsService.getLimits(monitoringId);

            assertEquals(dto, result);
        }

        @Test
        void throwsNotFound_whenMonitoringMissing() {
            UUID monitoringId = UUID.randomUUID();

            when(roomMonitoringRepository.findById(monitoringId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> climateStatsService.getLimits(monitoringId));
        }
    }
}