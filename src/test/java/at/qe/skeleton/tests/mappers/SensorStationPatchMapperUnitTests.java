package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.SensorStationPatchDTO;
import at.qe.skeleton.mappers.SensorStationPatchMapper;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SensorStationPatchMapper")
class SensorStationPatchMapperUnitTests {

    private RoomMonitoringRepository repository;
    private SensorStationPatchMapper mapper;

    @BeforeEach
    void setUp() {
        repository = mock(RoomMonitoringRepository.class);
        mapper = new SensorStationPatchMapper(repository);
    }

    @Nested
    @DisplayName("mapTo(SensorStation) — unsupported direction")
    class MapTo {

        @Test
        @DisplayName("throws UnsupportedOperationException")
        void throwsUnsupportedOperationException() {
            assertThrows(UnsupportedOperationException.class, () -> mapper.mapTo(new SensorStation()));
        }

        @Test
        @DisplayName("throws UnsupportedOperationException even when entity is null")
        void throwsEvenForNullEntity() {
            assertThrows(UnsupportedOperationException.class, () -> mapper.mapTo(null));
        }
    }

    @Nested
    @DisplayName("mapFrom(SensorStationPatchDTO) — DTO to Entity")
    class MapFrom {

        @Test
        @DisplayName("maps all non-null fields correctly")
        void mapsAllFields() {
            LocalDateTime now = LocalDateTime.now();
            UUID roomId = UUID.randomUUID();
            RoomMonitoring room = new RoomMonitoring();
            room.setRoomId(roomId);
            when(repository.findById(roomId)).thenReturn(Optional.of(room));

            SensorStation result = mapper.mapFrom(
                    new SensorStationPatchDTO("Updated-Station", now, DeviceStatus.ONLINE, roomId));

            assertThat(result.getName()).isEqualTo("Updated-Station");
            assertThat(result.getStatus()).isEqualTo(DeviceStatus.ONLINE);
            assertThat(result.getLastHeartBeat()).isEqualTo(now);
            assertThat(result.getRoomMonitoring()).isEqualTo(room);
        }

        @Test
        @DisplayName("preserves nulls when all dto fields are null")
        void preservesNulls() {
            SensorStation result = mapper.mapFrom(
                    new SensorStationPatchDTO(null, null, null, null));

            assertThat(result.getName()).isNull();
            assertThat(result.getStatus()).isNull();
            assertThat(result.getLastHeartBeat()).isNull();
            assertThat(result.getRoomMonitoring()).isNull();
        }

        @Test
        @DisplayName("sets roomMonitoring to null when roomId not found")
        void setsRoomToNullWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            SensorStation result = mapper.mapFrom(
                    new SensorStationPatchDTO("Station", null, null, unknownId));

            assertThat(result.getRoomMonitoring()).isNull();
        }

        @Test
        @DisplayName("throws NullPointerException when dto is null")
        void throwsOnNullDto() {
            assertThrows(NullPointerException.class, () -> mapper.mapFrom(null));
        }
    }
}