package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.mappers.SensorStationMapper;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.SensorStation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SensorStationMapper")
class SensorStationMapperUnitTests {

    private SensorStationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SensorStationMapper();
    }

    private SensorStation buildStation(UUID readId, UUID writeId, String name, DeviceStatus status,
                                       RoomMonitoring roomMonitoring) {
        SensorStation station = new SensorStation();
        station.setReadId(readId);
        station.setWriteId(writeId);
        station.setName(name);
        station.setStatus(status);
        station.setRoomMonitoring(roomMonitoring);
        return station;
    }

    private RoomMonitoring buildRoom(UUID roomId, RaspberryPi raspberryPi) {
        RoomMonitoring room = new RoomMonitoring();
        room.setRoomId(roomId);
        room.setRaspberryPi(raspberryPi);
        return room;
    }

    private RaspberryPi buildPi(UUID piId) {
        RaspberryPi pi = new RaspberryPi();
        pi.setId(piId);
        return pi;
    }

    @Nested
    @DisplayName("mapTo(SensorStation) — Entity to DTO")
    class MapTo {

        @Test
        @DisplayName("maps all fields correctly when roomMonitoring and raspberryPi are present")
        void mapsAllFieldsWithRoomAndPi() {
            UUID readId = UUID.randomUUID();
            UUID writeId = UUID.randomUUID();
            UUID roomId = UUID.randomUUID();
            UUID piId = UUID.randomUUID();
            RaspberryPi pi = buildPi(piId);
            RoomMonitoring room = buildRoom(roomId, pi);
            SensorStation station = buildStation(readId, writeId,"Station Alpha", DeviceStatus.ONLINE, room);

            SensorStationDTO result = mapper.mapTo(station);

            assertThat(result).isNotNull();
            assertThat(result.readId()).isEqualTo(readId);
            assertThat(result.writeId()).isEqualTo(writeId);
            assertThat(result.name()).isEqualTo("Station Alpha");
            assertThat(result.status()).isEqualTo(DeviceStatus.ONLINE);
            assertThat(result.roomId()).isEqualTo(roomId);
            assertThat(result.connectedToPiId()).isEqualTo(piId);
        }

        @Test
        @DisplayName("maps roomId and connectedToPiId to null when roomMonitoring is null")
        void mapsNullWhenNoRoom() {
            UUID readId = UUID.randomUUID();
            UUID writeId = UUID.randomUUID();
            SensorStation station = buildStation(readId, writeId, "Station Alpha", DeviceStatus.OFFLINE, null);

            SensorStationDTO result = mapper.mapTo(station);

            assertThat(result.readId()).isEqualTo(readId);
            assertThat(result.writeId()).isEqualTo(writeId);
            assertThat(result.name()).isEqualTo("Station Alpha");
            assertThat(result.status()).isEqualTo(DeviceStatus.OFFLINE);
            assertThat(result.roomId()).isNull();
            assertThat(result.connectedToPiId()).isNull();
        }

        @Test
        @DisplayName("maps connectedToPiId to null when roomMonitoring exists but raspberryPi is null")
        void mapsNullPiWhenRoomHasNoPi() {
            UUID readId = UUID.randomUUID();
            UUID writeId = UUID.randomUUID();
            UUID roomId = UUID.randomUUID();
            RoomMonitoring room = buildRoom(roomId, null);
            SensorStation station = buildStation(readId, writeId, "Station Alpha", DeviceStatus.ONLINE, room);

            SensorStationDTO result = mapper.mapTo(station);

            assertThat(result.roomId()).isEqualTo(roomId);
            assertThat(result.connectedToPiId()).isNull();
        }

        @Test
        @DisplayName("maps OFFLINE status correctly")
        void mapsOfflineStatus() {
            UUID readId = UUID.randomUUID();
            UUID writeId = UUID.randomUUID();
            SensorStation station = buildStation(readId, writeId, "Station", DeviceStatus.OFFLINE, null);

            SensorStationDTO result = mapper.mapTo(station);

            assertThat(result.status()).isEqualTo(DeviceStatus.OFFLINE);
        }

        @Test
        @DisplayName("maps ONLINE status correctly")
        void mapsOnlineStatus() {
            UUID readId = UUID.randomUUID();
            UUID writeId = UUID.randomUUID();
            SensorStation station = buildStation(readId, writeId, "Station", DeviceStatus.ONLINE, null);

            SensorStationDTO result = mapper.mapTo(station);

            assertThat(result.status()).isEqualTo(DeviceStatus.ONLINE);
        }

        @Test
        @DisplayName("throws NullPointerException when entity is null")
        void throwsOnNullEntity() {
            assertThrows(NullPointerException.class, () -> mapper.mapTo(null));
        }
    }

    @Nested
    @DisplayName("mapFrom(SensorStationDTO) — unsupported direction")
    class MapFrom {

        @Test
        @DisplayName("throws UnsupportedOperationException — read DTO is not used for writes")
        void throwsUnsupportedOperationException() {
            UUID readId = UUID.randomUUID();
            UUID writeId = UUID.randomUUID();
            SensorStationDTO dto = new SensorStationDTO(
                    readId,
                    writeId,
                    "Station Alpha",
                    DeviceStatus.OFFLINE,
                    UUID.randomUUID(),
                    UUID.randomUUID()
            );

            assertThrows(UnsupportedOperationException.class, () -> mapper.mapFrom(dto));
        }

        @Test
        @DisplayName("throws UnsupportedOperationException even when dto is null")
        void throwsEvenForNullDto() {
            assertThrows(UnsupportedOperationException.class, () -> mapper.mapFrom(null));
        }
    }
}