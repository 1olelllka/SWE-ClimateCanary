package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.SensorStationCreateDTO;
import at.qe.skeleton.mappers.SensorStationCreateMapper;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.SensorStation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SensorStationCreateMapper")
class SensorStationCreateMapperUnitTests {

    private SensorStationCreateMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SensorStationCreateMapper();
    }

    @Nested
    @DisplayName("mapTo(SensorStation) — unsupported direction")
    class MapTo {

        @Test
        @DisplayName("throws UnsupportedOperationException — create DTO is write-only")
        void throwsUnsupportedOperationException() {
            assertThatThrownBy(() -> mapper.mapTo(new SensorStation()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("throws UnsupportedOperationException even when entity is null")
        void throwsEvenForNullEntity() {
            assertThrows(UnsupportedOperationException.class,
                    () -> mapper.mapTo(null));
        }
    }
    @Nested
    @DisplayName("mapFrom(SensorStationCreateDTO) — DTO to Entity")
    class MapFrom {

        @Test
        @DisplayName("maps name correctly")
        void mapsName() {
            SensorStationCreateDTO dto = new SensorStationCreateDTO(
                    "Station Alpha",
                    UUID.randomUUID()
            );

            SensorStation result = mapper.mapFrom(dto);

            assertThat(result.getName()).isEqualTo("Station Alpha");
        }

        @Test
        @DisplayName("sets initial status to OFFLINE")
        void setsStatusToOffline() {
            SensorStationCreateDTO dto = new SensorStationCreateDTO(
                    "Station Alpha",
                    UUID.randomUUID()
            );

            SensorStation result = mapper.mapFrom(dto);

            assertThat(result.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
        }

        @Test
        @DisplayName("sets lastHeartBeat to null — station has never connected")
        void setsLastHeartBeatToNull() {
            SensorStationCreateDTO dto = new SensorStationCreateDTO(
                    "Station Alpha",
                    UUID.randomUUID()
            );

            SensorStation result = mapper.mapFrom(dto);

            assertThat(result.getLastHeartBeat()).isNull();
        }

        @Test
        @DisplayName("does not set id — left for JPA generation")
        void doesNotSetId() {
            SensorStationCreateDTO dto = new SensorStationCreateDTO(
                    "Station Alpha",
                    UUID.randomUUID()
            );

            SensorStation result = mapper.mapFrom(dto);

            assertThat(result.getId()).isNull();
        }

        @Test
        @DisplayName("does not set roomMonitoring — left for service layer via roomId")
        void doesNotSetRoomMonitoring() {
            SensorStationCreateDTO dto = new SensorStationCreateDTO(
                    "Station Alpha",
                    UUID.randomUUID()
            );

            SensorStation result = mapper.mapFrom(dto);

            assertThat(result.getRoomMonitoring()).isNull();
        }

        @Test
        @DisplayName("roomId and connectedToPiId are not mapped — resolved by service layer")
        void externalReferencesNotMapped() {
            UUID roomId = UUID.randomUUID();
            UUID piId = UUID.randomUUID();
            SensorStationCreateDTO dto = new SensorStationCreateDTO(
                    "Station Alpha",
                    roomId
            );

            SensorStation result = mapper.mapFrom(dto);

            assertThat(result.getRoomMonitoring()).isNull();
        }

        @Test
        @DisplayName("throws NullPointerException when dto is null")
        void throwsOnNullDto() {
            assertThrows(NullPointerException.class, () -> mapper.mapFrom(null));
        }
    }
}
