//package at.qe.skeleton.tests.services;
//
//import at.qe.skeleton.dtos.*;
//import at.qe.skeleton.mappers.WarningCreateMapper;
//import at.qe.skeleton.mappers.WarningMapper;
//import at.qe.skeleton.model.*;
//import at.qe.skeleton.repositories.*;
//import at.qe.skeleton.services.impl.WarningServiceImpl;
//import jakarta.persistence.EntityNotFoundException;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("WarningService")
//class WarningServiceUnitTests {
//
//    @Mock WarningRepository        warningsRepository;
//    @Mock RoomMonitoringRepository roomMonitoringRepository;
//
//    @Spy WarningMapper       warningMapper       = new WarningMapper();
//    @Spy WarningCreateMapper warningCreateMapper = new WarningCreateMapper();
//
//    @InjectMocks WarningServiceImpl service;
//
//
//    private final UUID          roomId    = UUID.randomUUID();
//    private final UUID          warningId = UUID.randomUUID();
//    private final LocalDateTime now       = LocalDateTime.of(2024, 6, 15, 12, 0);
//
//    private RoomMonitoring room() {
//        return RoomMonitoring.builder().roomId(roomId).roomNumber("A101").build();
//    }
//
//    private Warnings activeWarning() {
//        return Warnings.builder()
//                .id(warningId)
//                .roomMonitoring(room())
//                .measurementType(MeasurementType.TEMPERATURE)
//                .status(WarningStatus.YELLOW)
//                .message("Too hot")
//                .triggeredValue(28.5)
//                .activeLimitAtTime(25.0)
//                .createdAt(now)
//                .resolvedAt(null)
//                .build();
//    }
//
//    private Warnings resolvedWarning() {
//        return activeWarning().toBuilder()
//                .resolvedAt(now.minusHours(1))
//                .status(WarningStatus.GREEN)
//                .build();
//    }
//
//
//    @Nested
//    @DisplayName("getAllActiveWarnings")
//    class GetAllActiveWarnings {
//
//        @Test
//        @DisplayName("returns mapped DTOs for all active warnings")
//        void returnsMappedDTOs() {
//            when(warningsRepository.findAllActive())
//                    .thenReturn(List.of(activeWarning(), activeWarning()));
//
//            List<WarningDTO> result = service.getAllActiveWarnings();
//
//            assertThat(result).hasSize(2)
//                    .allMatch(WarningDTO::active);
//        }
//
//        @Test
//        @DisplayName("returns empty list when no active warnings exist")
//        void returnsEmptyList() {
//            when(warningsRepository.findAllActive()).thenReturn(List.of());
//
//            assertThat(service.getAllActiveWarnings()).isEmpty();
//        }
//    }
//
//    @Nested
//    @DisplayName("getAllWarningsForRoom")
//    class GetActiveWarningsForRoom {
//
//        @Test
//        @DisplayName("returns only active warnings for the given room")
//        void returnsActiveForRoom() {
//            when(warningsRepository
//                    .findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId))
//                    .thenReturn(List.of(activeWarning()));
//
//            List<WarningDTO> result = service.getAllWarningsForRoom(roomId);
//
//            assertThat(result).hasSize(1);
//            assertThat(result.get(0).roomId()).isEqualTo(roomId);
//            assertThat(result.get(0).active()).isTrue();
//        }
//
//        @Test
//        @DisplayName("returns empty list when room has no active warnings")
//        void returnsEmptyForCleanRoom() {
//            when(warningsRepository
//                    .findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId))
//                    .thenReturn(List.of());
//
//            assertThat(service.getAllWarningsForRoom(roomId)).isEmpty();
//        }
//    }
//
//    @Nested
//    @DisplayName("createWarning")
//    class CreateWarning {
//
//        private WarningCreateDTO dto() {
//            return new WarningCreateDTO(
//                    roomId, MeasurementType.TEMPERATURE,
//                    WarningStatus.YELLOW, 28.5, 25.0, "Too hot");
//        }
//
//        @Test
//        @DisplayName("persists warning with correct fields and returns mapped DTO")
//        void persistsAndReturnsMapped() {
//            when(roomMonitoringRepository.findById(roomId))
//                    .thenReturn(Optional.of(room()));
//            when(warningsRepository.save(any())).thenAnswer(i -> {
//                Warnings w = i.getArgument(0);
//                // simulate JPA assigning an ID
//                return w.toBuilder().id(warningId).build();
//            });
//
//            WarningDTO result = service.createWarning(dto());
//
//            assertThat(result.id()).isEqualTo(warningId);
//            assertThat(result.roomId()).isEqualTo(roomId);
//            assertThat(result.status()).isEqualTo(WarningStatus.YELLOW);
//            assertThat(result.triggeredValue()).isEqualTo(28.5);
//            assertThat(result.resolvedAt()).isNull();
//            assertThat(result.active()).isTrue();
//        }
//
//        @Test
//        @DisplayName("sets roomMonitoring from repository, not from DTO stub")
//        void setsRoomFromRepository() {
//            RoomMonitoring realRoom = room();
//            when(roomMonitoringRepository.findById(roomId))
//                    .thenReturn(Optional.of(realRoom));
//            when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//            service.createWarning(dto());
//
//            ArgumentCaptor<Warnings> captor = ArgumentCaptor.forClass(Warnings.class);
//            verify(warningsRepository).save(captor.capture());
//            assertThat(captor.getValue().getRoomMonitoring()).isSameAs(realRoom);
//        }
//
//        @Test
//        @DisplayName("throws EntityNotFoundException when room does not exist")
//        void throwsWhenRoomNotFound() {
//            when(roomMonitoringRepository.findById(roomId)).thenReturn(Optional.empty());
//
//            WarningCreateDTO dto = dto();
//
//            assertThatThrownBy(() -> service.createWarning(dto))
//                    .isInstanceOf(EntityNotFoundException.class)
//                    .hasMessageContaining(roomId.toString());
//
//            verify(warningsRepository, never()).save(any());
//        }
//    }
//
//
//    @Nested
//    @DisplayName("updateWarningStatus")
//    class UpdateWarningStatus {
//
//        @Test
//        @DisplayName("updates status and triggeredValue on an active warning")
//        void updatesFields() {
//            when(warningsRepository.findById(warningId))
//                    .thenReturn(Optional.of(activeWarning()));
//            when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//            WarningUpdateStatusDTO dto =
//                    new WarningUpdateStatusDTO(WarningStatus.RED, 35.0);
//
//            WarningDTO result = service.updateWarningStatus(warningId, dto);
//
//            assertThat(result.status()).isEqualTo(WarningStatus.RED);
//            assertThat(result.triggeredValue()).isEqualTo(35.0);
//        }
//
//        @Test
//        @DisplayName("throws EntityNotFoundException when warning does not exist")
//        void throwsWhenNotFound() {
//            when(warningsRepository.findById(warningId)).thenReturn(Optional.empty());
//
//            WarningUpdateStatusDTO dto = new WarningUpdateStatusDTO(WarningStatus.RED, 35.0);
//
//            assertThatThrownBy(() -> service.updateWarningStatus(warningId, dto))
//                    .isInstanceOf(EntityNotFoundException.class)
//                    .hasMessageContaining(warningId.toString());
//        }
//
//        @Test
//        @DisplayName("throws IllegalStateException when warning is already resolved")
//        void throwsWhenAlreadyResolved() {
//            when(warningsRepository.findById(warningId))
//                    .thenReturn(Optional.of(resolvedWarning()));
//
//            WarningUpdateStatusDTO dto = new WarningUpdateStatusDTO(WarningStatus.RED, 35.0);
//
//            assertThatThrownBy(() -> service.updateWarningStatus(warningId, dto))
//                    .isInstanceOf(IllegalStateException.class)
//                    .hasMessageContaining(warningId.toString());
//        }
//    }
//
//
//    @Nested
//    @DisplayName("resolveWarning")
//    class ResolveWarning {
//
//        @Test
//        @DisplayName("sets resolvedAt and status GREEN on an active warning")
//        void resolvesWarning() {
//            when(warningsRepository.findById(warningId))
//                    .thenReturn(Optional.of(activeWarning()));
//            when(warningsRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//            WarningDTO result = service.resolveWarning(warningId);
//
//            assertThat(result.status()).isEqualTo(WarningStatus.GREEN);
//            assertThat(result.resolvedAt()).isNotNull();
//            assertThat(result.active()).isFalse();
//        }
//
//        @Test
//        @DisplayName("throws EntityNotFoundException when warning does not exist")
//        void throwsWhenNotFound() {
//            when(warningsRepository.findById(warningId)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> service.resolveWarning(warningId))
//                    .isInstanceOf(EntityNotFoundException.class);
//        }
//
//        @Test
//        @DisplayName("throws IllegalStateException when warning is already resolved")
//        void throwsWhenAlreadyResolved() {
//            when(warningsRepository.findById(warningId))
//                    .thenReturn(Optional.of(resolvedWarning()));
//
//            assertThatThrownBy(() -> service.resolveWarning(warningId))
//                    .isInstanceOf(IllegalStateException.class)
//                    .hasMessageContaining(warningId.toString());
//        }
//    }
//
//
//    @Nested
//    @DisplayName("getViolationLog")
//    class GetViolationLog {
//
//        @Test
//        @DisplayName("returns both active and resolved warnings for the room")
//        void returnsMixed() {
//            when(warningsRepository.findByRoomMonitoring_RoomId(roomId))
//                    .thenReturn(List.of(activeWarning(), resolvedWarning()));
//
//            List<WarningDTO> result = service.getViolationLog(user, roomId);
//
//            assertThat(result).hasSize(2);
//            assertThat(result).extracting(WarningDTO::active)
//                    .containsExactlyInAnyOrder(true, false);
//        }
//
//        @Test
//        @DisplayName("returns empty list when room has no warnings")
//        void returnsEmptyForCleanRoom() {
//            when(warningsRepository.findByRoomMonitoring_RoomId(roomId))
//                    .thenReturn(List.of());
//
//            assertThat(service.getViolationLog(user, roomId)).isEmpty();
//        }
//    }
//}
