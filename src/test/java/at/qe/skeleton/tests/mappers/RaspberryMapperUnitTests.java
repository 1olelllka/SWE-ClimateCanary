package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.RaspberryDTO;
import at.qe.skeleton.mappers.RaspberryMapper;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("RaspberryMapper")
class RaspberryMapperUnitTests {

    private RaspberryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RaspberryMapper();
    }


    private RaspberryPi buildPi(UUID id, String name, String ip, DeviceStatus status,
                                RoomMonitoring roomMonitoring) {
        RaspberryPi pi = new RaspberryPi();
        pi.setId(id);
        pi.setName(name);
        pi.setIp(ip);
        pi.setStatus(status);
        pi.setRoomMonitoring(roomMonitoring);
        return pi;
    }

    private RoomMonitoring buildRoom(UUID roomId, String roomNumber) {
        RoomMonitoring room = new RoomMonitoring();
        room.setRoomId(roomId);
        room.setRoomNumber(roomNumber);
        return room;
    }


    @Nested
    @DisplayName("mapTo(RaspberryPi) — Entity to DTO")
    class MapTo {

        @Test
        @DisplayName("maps all fields correctly when roomMonitoring is present")
        void mapsAllFieldsWithRoom() {
            UUID piId = UUID.randomUUID();
            UUID roomId = UUID.randomUUID();
            RoomMonitoring room = buildRoom(roomId, "A.101");
            RaspberryPi pi = buildPi(piId, "Pi Lab A", "192.168.1.100", DeviceStatus.ONLINE, room);

            RaspberryDTO result = mapper.mapTo(pi);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(piId);
            assertThat(result.name()).isEqualTo("Pi Lab A");
            assertThat(result.ipAddress()).isEqualTo("192.168.1.100");
            assertThat(result.status()).isEqualTo(DeviceStatus.ONLINE);
            assertThat(result.roomId()).isEqualTo(roomId);
            assertThat(result.roomNumber()).isEqualTo("A.101");
        }

        @Test
        @DisplayName("maps roomId and roomNumber to null when roomMonitoring is null")
        void mapsNullWhenNoRoom() {
            UUID piId = UUID.randomUUID();
            RaspberryPi pi = buildPi(piId, "Pi Lab A", "192.168.1.100", DeviceStatus.OFFLINE, null);

            RaspberryDTO result = mapper.mapTo(pi);

            assertThat(result.id()).isEqualTo(piId);
            assertThat(result.name()).isEqualTo("Pi Lab A");
            assertThat(result.ipAddress()).isEqualTo("192.168.1.100");
            assertThat(result.status()).isEqualTo(DeviceStatus.OFFLINE);
            assertThat(result.roomId()).isNull();
            assertThat(result.roomNumber()).isNull();
        }

        @Test
        @DisplayName("maps OFFLINE status correctly")
        void mapsOfflineStatus() {
            RaspberryPi pi = buildPi(UUID.randomUUID(), "Pi", "10.0.0.1", DeviceStatus.OFFLINE, null);

            RaspberryDTO result = mapper.mapTo(pi);

            assertThat(result.status()).isEqualTo(DeviceStatus.OFFLINE);
        }

        @Test
        @DisplayName("maps ONLINE status correctly")
        void mapsOnlineStatus() {
            RaspberryPi pi = buildPi(UUID.randomUUID(), "Pi", "10.0.0.1", DeviceStatus.ONLINE, null);

            RaspberryDTO result = mapper.mapTo(pi);

            assertThat(result.status()).isEqualTo(DeviceStatus.ONLINE);
        }

        @Test
        @DisplayName("maps roomMonitoring with null roomNumber correctly")
        void mapsRoomWithNullRoomNumber() {
            UUID roomId = UUID.randomUUID();
            RoomMonitoring room = buildRoom(roomId, null);
            RaspberryPi pi = buildPi(UUID.randomUUID(), "Pi", "10.0.0.1", DeviceStatus.ONLINE, room);

            RaspberryDTO result = mapper.mapTo(pi);

            assertThat(result.roomId()).isEqualTo(roomId);
            assertThat(result.roomNumber()).isNull();
        }

        @Test
        @DisplayName("throws NullPointerException when entity is null")
        void throwsOnNullEntity() {
            assertThrows(NullPointerException.class, () -> mapper.mapTo(null));
        }
    }


    @Nested
    @DisplayName("mapFrom(RaspberryDTO) — unsupported direction")
    class MapFrom {

        @Test
        @DisplayName("throws UnsupportedOperationException — read DTO is not used for writes")
        void throwsUnsupportedOperationException() {
            RaspberryDTO dto = new RaspberryDTO(
                    UUID.randomUUID(), "Pi Lab A", "192.168.1.100",
                    DeviceStatus.OFFLINE, UUID.randomUUID(), "A.101"
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
