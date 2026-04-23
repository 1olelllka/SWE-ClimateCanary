package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.RaspberryDTO;
import at.qe.skeleton.dtos.RoomRaspberry;
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
    void setUp() { mapper = new RaspberryMapper(); }

    private RaspberryPi buildPi(UUID id, String name, String ip,
                                DeviceStatus status, RoomMonitoring room) {
        return RaspberryPi.builder()
                .id(id).name(name).ip(ip).status(status)
                .roomMonitoring(room)
                .build();
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
        @DisplayName("maps all fields correctly with assigned room")
        void mapsAllFieldsWithRoom() {
            UUID piId = UUID.randomUUID();
            UUID roomId = UUID.randomUUID();
            RoomMonitoring room = buildRoom(roomId, "A.101");
            RaspberryPi pi = buildPi(piId, "Pi Lab A", "192.168.1.100", DeviceStatus.ONLINE, room);

            RaspberryDTO result = mapper.mapTo(pi);

            assertThat(result.id()).isEqualTo(piId);
            assertThat(result.name()).isEqualTo("Pi Lab A");
            assertThat(result.ipAddress()).isEqualTo("192.168.1.100");
            assertThat(result.status()).isEqualTo(DeviceStatus.ONLINE);
            assertThat(result.room()).isNotNull();
            assertThat(result.room().roomId()).isEqualTo(roomId);
            assertThat(result.room().roomName()).isEqualTo("A.101");
        }

        @Test
        @DisplayName("maps room to null when no room assigned")
        void mapsNullWhenNoRoom() {
            RaspberryPi pi = buildPi(UUID.randomUUID(), "Pi", "10.0.0.1",
                    DeviceStatus.OFFLINE, null);

            RaspberryDTO result = mapper.mapTo(pi);

            assertThat(result.room()).isNull();
        }

        @Test
        @DisplayName("maps room with null roomNumber correctly")
        void mapsRoomWithNullRoomNumber() {
            UUID roomId = UUID.randomUUID();
            RaspberryPi pi = buildPi(UUID.randomUUID(), "Pi", "10.0.0.1",
                    DeviceStatus.ONLINE, buildRoom(roomId, null));

            RaspberryDTO result = mapper.mapTo(pi);

            assertThat(result.room()).isNotNull();
            assertThat(result.room().roomId()).isEqualTo(roomId);
            assertThat(result.room().roomName()).isNull();
        }

        @Test
        @DisplayName("maps OFFLINE status correctly")
        void mapsOfflineStatus() {
            RaspberryPi pi = buildPi(UUID.randomUUID(), "Pi", "10.0.0.1",
                    DeviceStatus.OFFLINE, null);

            assertThat(mapper.mapTo(pi).status()).isEqualTo(DeviceStatus.OFFLINE);
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
        @DisplayName("throws UnsupportedOperationException")
        void throwsUnsupportedOperationException() {
            RaspberryDTO dto = new RaspberryDTO(
                    UUID.randomUUID(), "Pi Lab A", "192.168.1.100", 1000,
                    DeviceStatus.OFFLINE, null
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
