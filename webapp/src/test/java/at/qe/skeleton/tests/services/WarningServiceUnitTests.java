package at.qe.skeleton.tests.services;

import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.dtos.WarningUpdateStatusDTO;
import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.mappers.WarningCreateMapper;
import at.qe.skeleton.mappers.WarningMapper;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.impl.WarningServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WarningService")
class WarningServiceUnitTests {

    @Mock
    WarningRepository warningsRepository;
    @Mock
    RoomMonitoringRepository roomMonitoringRepository;
    @Mock
    RoomRepository roomRepository;
    @Mock
    DepartmentRepository departmentRepository;
    @Mock
    TipRepository tipRepository;

    @Spy
    WarningMapper warningMapper = new WarningMapper();
    @Spy
    WarningCreateMapper warningCreateMapper = new WarningCreateMapper();

    @InjectMocks
    WarningServiceImpl service;

    private final UUID roomId = UUID.randomUUID();
    private final UUID warningId = UUID.randomUUID();
    private final UUID deptId = UUID.randomUUID();

    private final LocalDateTime now = LocalDateTime.of(2024, 6, 15, 12, 0);
    private final LocalDate startDate = LocalDate.of(2024, 6, 1);
    private final LocalDate endDate = LocalDate.of(2024, 6, 30);

    private Department department;
    private Room ownRoom;
    private Room otherRoom;

    @BeforeEach
    void setup() {
        department = new Department();
        department.setId(deptId);

        ownRoom = new Room();
        ownRoom.setId(roomId);
        ownRoom.setDepartment(department);

        otherRoom = new Room();
        otherRoom.setId(UUID.randomUUID());
        Department otherDepartment = new Department();
        otherDepartment.setId(UUID.randomUUID());

        otherRoom.setDepartment(otherDepartment);
        department.setRooms(List.of(ownRoom));
    }

    // ───── helpers ─────

    private RoomMonitoring roomMonitoring() {
        TemperatureLimit tempLimit = new TemperatureLimit();
        tempLimit.setMaxVal(25.0f);

        HumidityLimit humLimit = new HumidityLimit();
        humLimit.setMaxVal(60.0f);

        PollutionLimit polLimit = new PollutionLimit();
        polLimit.setMaxVal(50.0f);

        return RoomMonitoring.builder()
                .roomId(roomId)
                .roomNumber("A101")
                .tempLimit(tempLimit)
                .humLimit(humLimit)
                .polLimit(polLimit)
                .build();
    }

    private Warnings activeWarning() {
        return Warnings.builder()
                .id(warningId)
                .roomMonitoring(roomMonitoring())
                .measurementType(MeasurementType.TEMPERATURE)
                .status(WarningStatus.YELLOW)
                .message("Too hot")
                .triggeredValue(28.5)
                .activeLimitAtTime(25.0)
                .createdAt(now)
                .resolvedAt(null)
                .build();
    }

    private Tip sampleTip() {
        return Tip.builder()
                .id(UUID.randomUUID())
                .msg("Open a window.")
                .violationStatus(WarningStatus.YELLOW)
                .violationType(ViolationType.OVER)
                .violatedSensor(ViolatedSensor.TEMPERATURE)
                .warnings(new ArrayList<>())
                .build();
    }

    private Warnings resolvedWarning() {
        return activeWarning().toBuilder()
                .resolvedAt(now.minusHours(1))
                .status(WarningStatus.GREEN)
                .build();
    }

    private WarningCreateDTO createDto() {
        return new WarningCreateDTO(
                roomId, "Test device", MeasurementType.TEMPERATURE,
                WarningStatus.YELLOW, 28.5, 25.0, "Too hot");
    }

    private Userx departmentViewer() {
        return userWithPermission(Permission.CAN_VIEW_OWN_DEPARTMENT_WARNINGS, ownRoom);
    }

    private Userx officeViewer() {
        return userWithPermission(Permission.CAN_VIEW_OWN_OFFICE_WARNINGS, ownRoom);
    }

    private Userx officeViewerOtherRoom() {
        return userWithPermission(Permission.CAN_VIEW_OWN_OFFICE_WARNINGS, otherRoom);
    }

    private Userx userWithPermission(Permission permission, Room room) {
        UserRole role = new UserRole();
        role.setPermissions(Set.of(permission));

        Userx user = new Userx();
        user.setUserRoles(Set.of(role));
        user.setMyRoom(room);

        return user;
    }

    // ───── getAllWarningsForRoom ─────

    @Test
    void departmentViewer_activeTrue_returnsList() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));
        when(warningsRepository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId))
                .thenReturn(List.of(activeWarning()));

        var result = service.getAllWarningsForRoom(departmentViewer(), roomId, true, null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void departmentViewer_dateFiltered_returnsList() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));
        when(warningsRepository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                eq(roomId),
                any(), any()))
                .thenReturn(List.of(activeWarning(), resolvedWarning()));

        var result = service.getAllWarningsForRoom(departmentViewer(), roomId, false, startDate, endDate);

        assertThat(result).hasSize(2);
    }

    @Test
    void departmentViewer_otherDepartment_throws() {
        when(roomRepository.findById(otherRoom.getId())).thenReturn(Optional.of(otherRoom));

        assertThatThrownBy(() ->
                service.getAllWarningsForRoom(departmentViewer(), otherRoom.getId(), true, null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void officeViewer_ownRoom_activeTrue_returns() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));
        when(warningsRepository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId))
                .thenReturn(List.of(activeWarning()));

        var result = service.getAllWarningsForRoom(officeViewer(), roomId, true, null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void officeViewer_activeFalse_returnsActiveWarnings() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));
        when(warningsRepository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId))
                .thenReturn(List.of(activeWarning()));

        var result = service.getAllWarningsForRoom(officeViewer(), roomId, false, startDate, endDate);

        assertThat(result).hasSize(1);
        verify(warningsRepository).findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId);
        verify(warningsRepository, never()).findByRoomMonitoring_RoomIdAndCreatedAtBetween(any(), any(), any());
    }

    @Test
    void roomNotFound_throws() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getAllWarningsForRoom(departmentViewer(), roomId, true, null, null))
                .isInstanceOf(NotFoundException.class);
    }

    // ───── createWarning ─────

// ── createWarning ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("throws EntityNotFoundException when room does not exist")
    void throwsWhenRoomNotFound() {
        when(roomMonitoringRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createWarning(createDto()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(roomId.toString());

        verify(warningsRepository, never()).save(any());
    }


    @Test
    @DisplayName("TEMPERATURE OVER — links matching tip and saves both")
    void temperature_over_linksTip() {
        // triggeredValue (28.5) > tempLimit.maxVal (25.0) → OVER
        WarningCreateDTO dto = new WarningCreateDTO(
                roomId, "Sensor-01", MeasurementType.TEMPERATURE,
                WarningStatus.YELLOW, 28.5, 25.0, "Too hot");

        Tip tip = sampleTip();
        when(roomMonitoringRepository.findById(roomId))
                .thenReturn(Optional.of(roomMonitoring()));
        when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.TEMPERATURE))
                .thenReturn(Optional.of(tip));
        when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WarningDTO result = service.createWarning(dto);

        assertThat(result.roomId()).isEqualTo(roomId);
        verify(tipRepository).findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.TEMPERATURE);
        verify(tipRepository).save(tip);

        ArgumentCaptor<Warnings> captor = ArgumentCaptor.forClass(Warnings.class);
        verify(warningsRepository).save(captor.capture());
        assertThat(captor.getValue().getTip()).isEqualTo(tip);
    }

    @Test
    @DisplayName("TEMPERATURE UNDER — links matching tip and saves both")
    void temperature_under_linksTip() {
        // triggeredValue (15.0) < tempLimit.maxVal (25.0) → UNDER
        WarningCreateDTO dto = new WarningCreateDTO(
                roomId, "Sensor-01", MeasurementType.TEMPERATURE,
                WarningStatus.YELLOW, 15.0, 20.0, "Too cold");

        Tip tip = sampleTip();
        when(roomMonitoringRepository.findById(roomId))
                .thenReturn(Optional.of(roomMonitoring()));
        when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.TEMPERATURE))
                .thenReturn(Optional.of(tip));
        when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createWarning(dto);

        verify(tipRepository).findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.TEMPERATURE);
        verify(tipRepository).save(tip);
    }

    @Test
    @DisplayName("TEMPERATURE — saves warning without tip when no matching tip exists")
    void temperature_noMatchingTip_savesWarningOnly() {
        WarningCreateDTO dto = new WarningCreateDTO(
                roomId, "Sensor-01", MeasurementType.TEMPERATURE,
                WarningStatus.YELLOW, 28.5, 25.0, "Too hot");

        when(roomMonitoringRepository.findById(roomId))
                .thenReturn(Optional.of(roomMonitoring()));
        when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                any(), any(), any()))
                .thenReturn(Optional.empty());
        when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createWarning(dto);

        verify(tipRepository, never()).save(any());

        ArgumentCaptor<Warnings> captor = ArgumentCaptor.forClass(Warnings.class);
        verify(warningsRepository).save(captor.capture());
        assertThat(captor.getValue().getTip()).isNull();
    }

    @Test
    @DisplayName("HUMIDITY OVER — links matching tip and saves both")
    void humidity_over_linksTip() {
        // triggeredValue (70.0) > humLimit.maxVal (60.0) → OVER
        WarningCreateDTO dto = new WarningCreateDTO(
                roomId, "Sensor-01", MeasurementType.HUMIDITY,
                WarningStatus.YELLOW, 70.0, 60.0, "Too humid");

        Tip tip = sampleTip();
        when(roomMonitoringRepository.findById(roomId))
                .thenReturn(Optional.of(roomMonitoring()));
        when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.HUMIDITY))
                .thenReturn(Optional.of(tip));
        when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createWarning(dto);

        verify(tipRepository).findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.HUMIDITY);
        verify(tipRepository).save(tip);
    }

    @Test
    @DisplayName("HUMIDITY UNDER — links matching tip and saves both")
    void humidity_under_linksTip() {
        // triggeredValue (30.0) < humLimit.maxVal (60.0) → UNDER
        WarningCreateDTO dto = new WarningCreateDTO(
                roomId, "Sensor-01", MeasurementType.HUMIDITY,
                WarningStatus.YELLOW, 30.0, 60.0, "Too dry");

        Tip tip = sampleTip();
        when(roomMonitoringRepository.findById(roomId))
                .thenReturn(Optional.of(roomMonitoring()));
        when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.HUMIDITY))
                .thenReturn(Optional.of(tip));
        when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createWarning(dto);

        verify(tipRepository).findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.HUMIDITY);
        verify(tipRepository).save(tip);
    }


    @Test
    @DisplayName("AIR OVER — links matching tip and saves both")
    void air_over_linksTip() {
        // triggeredValue (60.0) > polLimit.maxVal (50.0) → OVER
        WarningCreateDTO dto = new WarningCreateDTO(
                roomId, "Sensor-01", MeasurementType.CO2,
                WarningStatus.RED, 60.0, 50.0, "Air quality critical");

        Tip tip = sampleTip();
        when(roomMonitoringRepository.findById(roomId))
                .thenReturn(Optional.of(roomMonitoring()));
        when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.RED, ViolationType.OVER, ViolatedSensor.AIR))
                .thenReturn(Optional.of(tip));
        when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createWarning(dto);

        verify(tipRepository).findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.RED, ViolationType.OVER, ViolatedSensor.AIR);
        verify(tipRepository).save(tip);
    }

    @Test
    @DisplayName("AIR UNDER — links matching tip and saves both")
    void air_under_linksTip() {
        // triggeredValue (20.0) < polLimit.maxVal (50.0) → UNDER
        WarningCreateDTO dto = new WarningCreateDTO(
                roomId, "Sensor-01", MeasurementType.CO2,
                WarningStatus.YELLOW, 20.0, 50.0, "Air quality low");

        Tip tip = sampleTip();
        when(roomMonitoringRepository.findById(roomId))
                .thenReturn(Optional.of(roomMonitoring()));
        when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.AIR))
                .thenReturn(Optional.of(tip));
        when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createWarning(dto);

        verify(tipRepository).findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.AIR);
        verify(tipRepository).save(tip);
    }

    @Test
    @DisplayName("always sets roomMonitoring on the warning before saving")
    void alwaysSetsRoomMonitoringBeforeSave() {
        when(roomMonitoringRepository.findById(roomId))
                .thenReturn(Optional.of(roomMonitoring()));
        when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                any(), any(), any()))
                .thenReturn(Optional.empty());
        when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createWarning(createDto());

        ArgumentCaptor<Warnings> captor = ArgumentCaptor.forClass(Warnings.class);
        verify(warningsRepository).save(captor.capture());
        assertThat(captor.getValue().getRoomMonitoring().getRoomId()).isEqualTo(roomId);
    }

    @Test
    void createWarning_roomNotFound() {
        when(roomMonitoringRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createWarning(createDto()))
                .isInstanceOf(NotFoundException.class);
    }

    // ───── updateWarningStatus ─────

    @Test
    void updateWarningStatus_success() {
        when(warningsRepository.findById(warningId)).thenReturn(Optional.of(activeWarning()));
        when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.updateWarningStatus(warningId,
                new WarningUpdateStatusDTO(WarningStatus.RED, 30));

        assertThat(result.status()).isEqualTo(WarningStatus.RED);
    }

    @Test
    void updateWarningStatus_notFound() {
        when(warningsRepository.findById(warningId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateWarningStatus(warningId, new WarningUpdateStatusDTO(WarningStatus.RED, 30)))
                .isInstanceOf(NotFoundException.class);
    }

    // ───── resolveWarning ─────

    @Test
    void resolveWarning_success() {
        when(warningsRepository.findById(warningId)).thenReturn(Optional.of(activeWarning()));
        when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.resolveWarning(warningId);

        assertThat(result.active()).isFalse();
    }

    // ───── getViolationLogForDepartment ─────

    @Test
    void departmentSummary_activeTrue() {
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(department));
        when(warningsRepository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNull(anyList()))
                .thenReturn(List.of(activeWarning()));

        var result = service.getViolationLogForDepartment(deptId, true, null, null);

        assertThat(result).hasSize(1);
    }

    // ───── getDetailedViolationLogForDepartment ─────

    @Test
    void detailedLog_success() {
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(department));
        when(warningsRepository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNullAndCreatedAtBetween(
                anyList(), any(), any()))
                .thenReturn(List.of(activeWarning()));

        var result = service.getDetailedViolationLogForDepartment(
                departmentViewer(), deptId, true, startDate, endDate);

        assertThat(result).hasSize(1);
    }

    @Test
    void detailedLog_otherDepartment_throws() {
        Department other = new Department();
        other.setId(UUID.randomUUID());

        when(departmentRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() ->
                service.getDetailedViolationLogForDepartment(
                        departmentViewer(), other.getId(), true, startDate, endDate))
                .isInstanceOf(ForbiddenException.class);
    }
}