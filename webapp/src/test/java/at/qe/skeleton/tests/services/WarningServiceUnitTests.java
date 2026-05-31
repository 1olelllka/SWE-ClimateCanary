package at.qe.skeleton.tests.services;

import at.qe.skeleton.dtos.ActiveViolationBuildingStats;
import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.dtos.WarningUpdateStatusDTO;
import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.WarningCreateMapper;
import at.qe.skeleton.mappers.WarningMapper;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.EmailService;
import at.qe.skeleton.services.LiveDataService;
import at.qe.skeleton.services.impl.WarningServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WarningServiceImpl")
class WarningServiceUnitTests {

    // ── repositories & services ──────────────────────────────────────────────
    @Mock WarningRepository        warningsRepository;
    @Mock RoomMonitoringRepository roomMonitoringRepository;
    @Mock RoomRepository           roomRepository;
    @Mock DepartmentRepository     departmentRepository;
    @Mock BuildingRepository       buildingRepository;
    @Mock TipRepository            tipRepository;
    @Mock LiveDataService          liveDataService;
    @Mock UserxRepository          userxRepository;
    @Mock UserSettingsRepository   userSettingsRepository;
    @Mock EmailService             emailService;

    @Spy WarningMapper       warningMapper       = new WarningMapper();
    @Spy WarningCreateMapper warningCreateMapper = new WarningCreateMapper();

    @InjectMocks WarningServiceImpl service;

    // ── shared IDs ───────────────────────────────────────────────────────────
    private final UUID roomId     = UUID.randomUUID();
    private final UUID warningId  = UUID.randomUUID();
    private final UUID deptId     = UUID.randomUUID();
    private final UUID buildingId = UUID.randomUUID();

    private final LocalDateTime now       = LocalDateTime.of(2024, 6, 15, 12, 0);
    private final LocalDate     startDate = LocalDate.of(2024, 6, 1);
    private final LocalDate     endDate   = LocalDate.of(2024, 6, 30);

    // ── domain objects ───────────────────────────────────────────────────────
    private Building   building;
    private Department department;
    private Room       ownRoom;
    private Room       otherRoom;

    @BeforeEach
    void setup() {
        building = new Building();
        building.setId(buildingId);

        department = new Department();
        department.setId(deptId);
        department.setBuilding(building);
        building.setDepartments(List.of(department));

        ownRoom = new Room();
        ownRoom.setId(roomId);
        ownRoom.setDepartment(department);
        ownRoom.setRoomType(RoomType.OFFICE);
        department.setRooms(List.of(ownRoom));

        Department otherDepartment = new Department();
        otherDepartment.setId(UUID.randomUUID());
        otherRoom = new Room();
        otherRoom.setId(UUID.randomUUID());
        otherRoom.setRoomType(RoomType.OFFICE);
        otherRoom.setDepartment(otherDepartment);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

    /** Returns a fresh active warning — always the same warningId so ID comparisons are stable. */
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

    private Warnings resolvedWarning() {
        return activeWarning().toBuilder()
                .id(UUID.randomUUID())
                .resolvedAt(now.minusHours(1))
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

    private WarningCreateDTO createDto() {
        return new WarningCreateDTO(
                roomId, MeasurementType.TEMPERATURE,
                WarningStatus.YELLOW, 28.5, 25.0, "Too hot", UUID.randomUUID());
    }

    // ── user factory helpers ─────────────────────────────────────────────────

    private Userx departmentViewer() {
        return userWithPermission(Permission.CAN_VIEW_OWN_DEPARTMENT_WARNINGS, ownRoom);
    }

    private Userx officeViewer() {
        return userWithPermission(Permission.CAN_VIEW_OWN_OFFICE_WARNINGS, ownRoom);
    }

    /** Building manager has no assigned room — exercises the second if-block in the service. */
    private Userx buildingViewer() {
        return userWithPermission(Permission.CAN_VIEW_ALL_ROOMS, null);
    }

    private Userx userWithPermission(Permission permission, Room room) {
        UserRole role = new UserRole();
        role.setPermissions(Set.of(permission));

        Userx user = new Userx();
        user.setUserRoles(Set.of(role));
        user.setMyRoom(room);
        return user;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getAllWarningsForRoom
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllWarningsForRoom")
    class GetAllWarningsForRoom {

        @Test
        @DisplayName("throws NotFoundException when room does not exist")
        void roomNotFound_throws() {
            when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.getAllWarningsForRoom(departmentViewer(), roomId, true, null, null))
                    .isInstanceOf(NotFoundException.class);
        }

        // ── department viewer ──

        @Test
        @DisplayName("department viewer | active=true | returns active warnings")
        void departmentViewer_activeTrue_returnsList() {
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));
            when(warningsRepository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId))
                    .thenReturn(List.of(activeWarning()));

            var result = service.getAllWarningsForRoom(departmentViewer(), roomId, true, null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("department viewer | active=false | returns date-filtered warnings")
        void departmentViewer_activeFalse_returnsDateFiltered() {
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));
            when(warningsRepository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                    eq(roomId), any(), any()))
                    .thenReturn(List.of(activeWarning(), resolvedWarning()));

            var result = service.getAllWarningsForRoom(departmentViewer(), roomId, false, startDate, endDate);

            assertThat(result).hasSize(2);
            verify(warningsRepository).findByRoomMonitoring_RoomIdAndCreatedAtBetween(eq(roomId), any(), any());
            verify(warningsRepository, never()).findByRoomMonitoring_RoomIdAndResolvedAtIsNull(any());
        }

        @Test
        @DisplayName("department viewer | room in another department | throws ForbiddenException")
        void departmentViewer_otherDepartment_throws() {
            when(roomRepository.findById(otherRoom.getId())).thenReturn(Optional.of(otherRoom));

            assertThatThrownBy(() ->
                    service.getAllWarningsForRoom(departmentViewer(), otherRoom.getId(), true, null, null))
                    .isInstanceOf(ForbiddenException.class);
        }

        // ── building viewer ──

        @Test
        @DisplayName("building viewer (no assigned room) | active=true | returns active warnings")
        void buildingViewer_activeTrue_returnsList() {
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));
            when(warningsRepository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId))
                    .thenReturn(List.of(activeWarning()));

            var result = service.getAllWarningsForRoom(buildingViewer(), roomId, true, null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("building viewer (no assigned room) | active=false | returns date-filtered warnings")
        void buildingViewer_activeFalse_returnsDateFiltered() {
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));
            when(warningsRepository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                    eq(roomId), any(), any()))
                    .thenReturn(List.of(activeWarning(), resolvedWarning()));

            var result = service.getAllWarningsForRoom(buildingViewer(), roomId, false, startDate, endDate);

            assertThat(result).hasSize(2);
        }

        // ── office viewer ──

        @Test
        @DisplayName("office viewer | own room | returns active warnings")
        void officeViewer_ownRoom_activeTrue_returns() {
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));
            when(warningsRepository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId))
                    .thenReturn(List.of(activeWarning()));

            var result = service.getAllWarningsForRoom(officeViewer(), roomId, true, null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("office viewer | active=false ignored | always returns only active warnings")
        void officeViewer_activeFlagIgnored_alwaysReturnsActiveOnly() {
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));
            when(warningsRepository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId))
                    .thenReturn(List.of(activeWarning()));

            var result = service.getAllWarningsForRoom(officeViewer(), roomId, false, startDate, endDate);

            assertThat(result).hasSize(1);
            verify(warningsRepository).findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId);
            verify(warningsRepository, never())
                    .findByRoomMonitoring_RoomIdAndCreatedAtBetween(any(), any(), any());
        }

        @Test
        @DisplayName("office viewer | different room, same department | throws ForbiddenException")
        void officeViewer_sameDepartmentRoom_returnsActiveWarnings() {
            Room siblingRoom = new Room();
            siblingRoom.setId(UUID.randomUUID());
            siblingRoom.setRoomType(RoomType.OFFICE);
            siblingRoom.setDepartment(department);   // same department as ownRoom

            when(roomRepository.findById(siblingRoom.getId())).thenReturn(Optional.of(siblingRoom));

            assertThrows(ForbiddenException.class, () -> service.getAllWarningsForRoom(officeViewer(), siblingRoom.getId(), true, null, null));
        }

        @Test
        @DisplayName("office viewer | room in different department | throws ForbiddenException")
        void officeViewer_differentDepartment_throwsForbidden() {
            when(roomRepository.findById(otherRoom.getId())).thenReturn(Optional.of(otherRoom));

            assertThatThrownBy(() ->
                    service.getAllWarningsForRoom(officeViewer(), otherRoom.getId(), true, null, null))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // createWarning
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createWarning")
    class CreateWarning {

        /** Stubs needed for every happy-path createWarning call. */
        private void stubRoomAndRepo() {
            when(roomMonitoringRepository.findById(roomId))
                    .thenReturn(Optional.of(roomMonitoring()));
            when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));
        }

        @Test
        @DisplayName("throws NotFoundException when RoomMonitoring does not exist")
        void throwsWhenRoomMonitoringNotFound() {
            when(roomMonitoringRepository.findById(roomId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createWarning(createDto()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(roomId.toString());

            verify(warningsRepository, never()).save(any());
        }

        @Test
        @DisplayName("TEMPERATURE OVER — links matching tip and saves both")
        void temperature_over_linksTip() {
            WarningCreateDTO dto = new WarningCreateDTO(
                    roomId, MeasurementType.TEMPERATURE,
                    WarningStatus.YELLOW, 28.5, 25.0, "Too hot", UUID.randomUUID());

            Tip tip = sampleTip();
            stubRoomAndRepo();
            when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.TEMPERATURE))
                    .thenReturn(Optional.of(tip));

            WarningDTO result = service.createWarning(dto);

            assertThat(result.roomId()).isEqualTo(roomId);
            verify(tipRepository).save(tip);

            ArgumentCaptor<Warnings> captor = ArgumentCaptor.forClass(Warnings.class);
            verify(warningsRepository).save(captor.capture());
            assertThat(captor.getValue().getTip()).isEqualTo(tip);
        }

        @Test
        @DisplayName("TEMPERATURE UNDER — links matching tip and saves both")
        void temperature_under_linksTip() {
            WarningCreateDTO dto = new WarningCreateDTO(
                    roomId, MeasurementType.TEMPERATURE,
                    WarningStatus.YELLOW, 15.0, 20.0, "Too cold", UUID.randomUUID());

            Tip tip = sampleTip();
            stubRoomAndRepo();
            when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.TEMPERATURE))
                    .thenReturn(Optional.of(tip));

            service.createWarning(dto);

            verify(tipRepository).findByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.TEMPERATURE);
            verify(tipRepository).save(tip);
        }

        @Test
        @DisplayName("TEMPERATURE — saves warning with null tip when no matching tip exists")
        void temperature_noMatchingTip_savesWarningWithNullTip() {
            stubRoomAndRepo();
            when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(any(), any(), any()))
                    .thenReturn(Optional.empty());

            service.createWarning(createDto());

            verify(tipRepository, never()).save(any());
            ArgumentCaptor<Warnings> captor = ArgumentCaptor.forClass(Warnings.class);
            verify(warningsRepository).save(captor.capture());
            assertThat(captor.getValue().getTip()).isNull();
        }

        @Test
        @DisplayName("HUMIDITY OVER — links matching tip")
        void humidity_over_linksTip() {
            WarningCreateDTO dto = new WarningCreateDTO(
                    roomId, MeasurementType.HUMIDITY,
                    WarningStatus.YELLOW, 70.0, 60.0, "Too humid", UUID.randomUUID());

            Tip tip = sampleTip();
            stubRoomAndRepo();
            when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.HUMIDITY))
                    .thenReturn(Optional.of(tip));

            service.createWarning(dto);

            verify(tipRepository).findByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.HUMIDITY);
            verify(tipRepository).save(tip);
        }

        @Test
        @DisplayName("HUMIDITY UNDER — links matching tip")
        void humidity_under_linksTip() {
            WarningCreateDTO dto = new WarningCreateDTO(
                    roomId, MeasurementType.HUMIDITY,
                    WarningStatus.YELLOW, 30.0, 60.0, "Too dry", UUID.randomUUID());

            Tip tip = sampleTip();
            stubRoomAndRepo();
            when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.HUMIDITY))
                    .thenReturn(Optional.of(tip));

            service.createWarning(dto);

            verify(tipRepository).findByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.HUMIDITY);
            verify(tipRepository).save(tip);
        }

        @Test
        @DisplayName("CO2 OVER — links matching AIR tip")
        void air_over_linksTip() {
            WarningCreateDTO dto = new WarningCreateDTO(
                    roomId, MeasurementType.CO2,
                    WarningStatus.RED, 60.0, 50.0, "Air quality critical", UUID.randomUUID());

            Tip tip = sampleTip();
            stubRoomAndRepo();
            when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.RED, ViolationType.OVER, ViolatedSensor.AIR))
                    .thenReturn(Optional.of(tip));

            service.createWarning(dto);

            verify(tipRepository).findByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.RED, ViolationType.OVER, ViolatedSensor.AIR);
            verify(tipRepository).save(tip);
        }

        @Test
        @DisplayName("CO2 UNDER — links matching AIR tip")
        void air_under_linksTip() {
            WarningCreateDTO dto = new WarningCreateDTO(
                    roomId, MeasurementType.CO2,
                    WarningStatus.YELLOW, 20.0, 50.0, "Air quality low", UUID.randomUUID());

            Tip tip = sampleTip();
            stubRoomAndRepo();
            when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.AIR))
                    .thenReturn(Optional.of(tip));

            service.createWarning(dto);

            verify(tipRepository).findByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.AIR);
            verify(tipRepository).save(tip);
        }

        @Test
        @DisplayName("always sets roomMonitoring on the warning before persisting")
        void alwaysSetsRoomMonitoringBeforeSave() {
            stubRoomAndRepo();
            when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(any(), any(), any()))
                    .thenReturn(Optional.empty());

            service.createWarning(createDto());

            ArgumentCaptor<Warnings> captor = ArgumentCaptor.forClass(Warnings.class);
            verify(warningsRepository).save(captor.capture());
            assertThat(captor.getValue().getRoomMonitoring().getRoomId()).isEqualTo(roomId);
        }

        @Test
        @DisplayName("pushes live-data events after successful save")
        void pushesLiveDataAfterSave() {
            stubRoomAndRepo();
            when(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(any(), any(), any()))
                    .thenReturn(Optional.empty());

            service.createWarning(createDto());

            verify(liveDataService).pushActiveWarning(eq(roomId), any(WarningDTO.class));
            verify(liveDataService).pushActiveWarningDepartment(eq(deptId), any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // updateWarningStatus
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateWarningStatus")
    class UpdateWarningStatus {

        @Test
        @DisplayName("updates status and triggeredValue when warning is active")
        void success_updatesFields() {
            when(warningsRepository.findById(warningId)).thenReturn(Optional.of(activeWarning()));
            when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var result = service.updateWarningStatus(warningId,
                    new WarningUpdateStatusDTO(WarningStatus.RED, 30));

            assertThat(result.status()).isEqualTo(WarningStatus.RED);
        }

        @Test
        @DisplayName("throws NotFoundException when warning does not exist")
        void notFound_throws() {
            when(warningsRepository.findById(warningId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.updateWarningStatus(warningId, new WarningUpdateStatusDTO(WarningStatus.RED, 30)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws ForbiddenException when warning is already resolved")
        void alreadyResolved_throws() {
            when(warningsRepository.findById(warningId)).thenReturn(Optional.of(resolvedWarning()));

            assertThatThrownBy(() ->
                    service.updateWarningStatus(warningId, new WarningUpdateStatusDTO(WarningStatus.RED, 30)))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("already resolved");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // resolveWarning
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resolveWarning")
    class ResolveWarning {

        @Test
        @DisplayName("sets resolvedAt and propagates live-data events")
        void success_setsResolvedAt() {
            Warnings warning = activeWarning();
            when(warningsRepository.findById(warningId)).thenReturn(Optional.of(warning));
            when(warningsRepository.findAllByRoomAndActiveByType(roomId, MeasurementType.TEMPERATURE))
                    .thenReturn(List.of(warning));
            when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));

            var result = service.resolveWarning(warningId);

            assertThat(result.active()).isFalse();
            verify(liveDataService).resolveActiveWarning(eq(roomId), any(WarningDTO.class));
            verify(liveDataService, atLeastOnce()).resolveActiveWarningDepartment(eq(deptId), any());
        }

        @Test
        @DisplayName("also resolves sibling active warnings of the same type")
        void resolvesAllSiblingWarnings() {
            Warnings primary   = activeWarning();
            Warnings sibling   = activeWarning().toBuilder().id(UUID.randomUUID()).build();

            when(warningsRepository.findById(warningId)).thenReturn(Optional.of(primary));
            when(warningsRepository.findAllByRoomAndActiveByType(roomId, MeasurementType.TEMPERATURE))
                    .thenReturn(List.of(primary, sibling));
            when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(ownRoom));

            service.resolveWarning(warningId);

            verify(warningsRepository, times(2)).save(any(Warnings.class));
        }

        @Test
        @DisplayName("throws NotFoundException when warning does not exist")
        void notFound_throws() {
            when(warningsRepository.findById(warningId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolveWarning(warningId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("throws ForbiddenException when warning is already resolved")
        void alreadyResolved_throws() {
            when(warningsRepository.findById(warningId)).thenReturn(Optional.of(resolvedWarning()));

            assertThatThrownBy(() -> service.resolveWarning(warningId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("throws ValidationException when warning has no RoomMonitoring")
        void noRoomMonitoring_throws() {
            Warnings detached = activeWarning().toBuilder().roomMonitoring(null).build();
            when(warningsRepository.findById(warningId)).thenReturn(Optional.of(detached));

            assertThatThrownBy(() -> service.resolveWarning(warningId))
                    .isInstanceOf(ValidationException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getViolationLogForDepartment
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getViolationLogForDepartment")
    class GetViolationLogForDepartment {

        @Test
        @DisplayName("active=true | returns active warnings for all rooms in department")
        void activeTrue_returnsActiveWarnings() {
            when(departmentRepository.findById(deptId)).thenReturn(Optional.of(department));
            when(warningsRepository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNull(anyList()))
                    .thenReturn(List.of(activeWarning()));

            var result = service.getViolationLogForDepartment(deptId, true, null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("active=false | returns date-filtered warnings for all rooms")
        void activeFalse_returnsDateFilteredWarnings() {
            when(departmentRepository.findById(deptId)).thenReturn(Optional.of(department));
            when(warningsRepository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                    eq(roomId), any(), any()))
                    .thenReturn(List.of(activeWarning(), resolvedWarning()));

            var result = service.getViolationLogForDepartment(deptId, false, startDate, endDate);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("throws NotFoundException when department does not exist")
        void departmentNotFound_throws() {
            when(departmentRepository.findById(deptId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.getViolationLogForDepartment(deptId, true, null, null))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getDetailedViolationLogForDepartment
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getDetailedViolationLogForDepartment")
    class GetDetailedViolationLogForDepartment {

        @Test
        @DisplayName("active=true | department member | returns matching warnings")
        void activeTrue_success() {
            when(departmentRepository.findById(deptId)).thenReturn(Optional.of(department));
            when(warningsRepository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNullAndCreatedAtBetween(
                    anyList(), any(), any()))
                    .thenReturn(List.of(activeWarning()));

            var result = service.getDetailedViolationLogForDepartment(
                    departmentViewer(), deptId, true, startDate, endDate);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("active=false | department member | returns date-filtered warnings")
        void activeFalse_success() {
            when(departmentRepository.findById(deptId)).thenReturn(Optional.of(department));
            when(warningsRepository.findByRoomMonitoring_RoomIdInAndCreatedAtBetween(
                    anyList(), any(), any()))
                    .thenReturn(List.of(activeWarning(), resolvedWarning()));

            var result = service.getDetailedViolationLogForDepartment(
                    departmentViewer(), deptId, false, startDate, endDate);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("throws ForbiddenException when user belongs to a different department")
        void otherDepartment_throws() {
            Department other = new Department();
            other.setId(UUID.randomUUID());
            when(departmentRepository.findById(other.getId())).thenReturn(Optional.of(other));

            assertThatThrownBy(() ->
                    service.getDetailedViolationLogForDepartment(
                            departmentViewer(), other.getId(), true, startDate, endDate))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("throws ForbiddenException when user has no assigned room")
        void noAssignedRoom_throws() {
            Userx userWithoutRoom = userWithPermission(Permission.CAN_VIEW_OWN_DEPARTMENT_WARNINGS, null);
            when(departmentRepository.findById(deptId)).thenReturn(Optional.of(department));

            assertThatThrownBy(() ->
                    service.getDetailedViolationLogForDepartment(
                            userWithoutRoom, deptId, true, startDate, endDate))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getActiveViolationsForBuilding
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getActiveViolationsForBuilding")
    class GetActiveViolationsForBuilding {

        @Test
        @DisplayName("throws NotFoundException when building does not exist")
        void buildingNotFound_throws() {
            when(buildingRepository.findById(buildingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getActiveViolationsForBuilding(buildingId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("returns correct active violation count")
        void returnsActiveViolationCount() {
            when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
            when(warningsRepository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNull(List.of(roomId)))
                    .thenReturn(List.of(activeWarning(), activeWarning()));

            ActiveViolationBuildingStats stats = service.getActiveViolationsForBuilding(buildingId);

            assertNotNull(stats);
            assertEquals(2, stats.activeViolations());
        }

        @Test
        @DisplayName("returns zero when no active violations exist")
        void returnsZeroWhenNoActiveViolations() {
            when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
            when(warningsRepository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNull(List.of(roomId)))
                    .thenReturn(List.of());

            ActiveViolationBuildingStats stats = service.getActiveViolationsForBuilding(buildingId);

            assertEquals(0, stats.activeViolations());
        }
    }
}