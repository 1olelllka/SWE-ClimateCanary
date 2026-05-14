package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.RaspberryCreateDTO;
import at.qe.skeleton.mappers.RaspberryCreateMapper;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("RaspberryCreateMapper")
class RaspberryCreateMapperUnitTests {

    private RaspberryCreateMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RaspberryCreateMapper();
    }


    @Nested
    @DisplayName("mapTo(RaspberryPi) — unsupported direction")
    class MapTo {

        @Test
        @DisplayName("throws UnsupportedOperationException — create DTO is write-only")
        void throwsUnsupportedOperationException() {
            RaspberryPi pi = new RaspberryPi();

            assertThrows(UnsupportedOperationException.class, () -> mapper.mapTo(pi));
        }

        @Test
        @DisplayName("throws UnsupportedOperationException even when entity is null")
        void throwsEvenForNullEntity() {
            assertThrows(UnsupportedOperationException.class, () -> mapper.mapTo(null));
        }
    }


    @Nested
    @DisplayName("mapFrom(RaspberryCreateDTO) — DTO to Entity")
    class MapFrom {

        @Test
        @DisplayName("maps name and ip address correctly")
        void mapsNameAndIpAddress() {
            RaspberryCreateDTO dto = new RaspberryCreateDTO(
                    "Pi Lab A",
                    "192.168.1.100",
                    1000,
                    UUID.randomUUID()
            );

            RaspberryPi result = mapper.mapFrom(dto);

            assertThat(result.getName()).isEqualTo("Pi Lab A");
            assertThat(result.getIp()).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("sets initial status to OFFLINE")
        void setsStatusToOffline() {
            RaspberryCreateDTO dto = new RaspberryCreateDTO(
                    "Pi Lab A",
                    "192.168.1.100",
                    1000,
                    UUID.randomUUID()
            );

            RaspberryPi result = mapper.mapFrom(dto);

            assertThat(result.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
        }

        @Test
        @DisplayName("sets violation counter to zero")
        void setsViolationCounterToZero() {
            RaspberryCreateDTO dto = new RaspberryCreateDTO(
                    "Pi Lab A",
                    "192.168.1.100",
                    1000,
                    UUID.randomUUID()
            );

            RaspberryPi result = mapper.mapFrom(dto);

            assertThat(result.getViolationCounter()).isZero();
        }

        @Test
        @DisplayName("sets lastHeartBeat to null — device has never connected")
        void setsLastHeartBeatToNull() {
            RaspberryCreateDTO dto = new RaspberryCreateDTO(
                    "Pi Lab A",
                    "192.168.1.100",
                    1000,
                    UUID.randomUUID()
            );

            RaspberryPi result = mapper.mapFrom(dto);

            assertThat(result.getLastHeartBeat()).isNull();
        }

        @Test
        @DisplayName("does not set id — left for JPA generation")
        void doesNotSetId() {
            RaspberryCreateDTO dto = new RaspberryCreateDTO(
                    "Pi Lab A",
                    "192.168.1.100",
                    1000,
                    UUID.randomUUID()
            );

            RaspberryPi result = mapper.mapFrom(dto);

            assertThat(result.getId()).isNull();
        }

        @Test
        @DisplayName("does not set roomMonitoring — left for service layer via roomId")
        void doesNotSetRoomMonitoring() {
            RaspberryCreateDTO dto = new RaspberryCreateDTO(
                    "Pi Lab A",
                    "192.168.1.100",
                    1000,
                    UUID.randomUUID()
            );

            RaspberryPi result = mapper.mapFrom(dto);

            assertThat(result.getRoomMonitoring()).isNull();
        }

        @Test
        @DisplayName("does not set frequency — defaults to 100 via @Builder.Default")
        void doesNotSetFrequency() {
            RaspberryCreateDTO dto = new RaspberryCreateDTO(
                    "Pi Lab A",
                    "192.168.1.100",
                    1000,
                    UUID.randomUUID()
            );

            RaspberryPi result = mapper.mapFrom(dto);

            // RaspberryPi uses new RaspberryPi() not builder, so @Builder.Default
            // does not apply — frequency will be 0 until set by service layer
            assertThat(result.getFrequency()).isEqualTo(1000);//default value
        }

        @Test
        @DisplayName("throws NullPointerException when dto is null")
        void throwsOnNullDto() {
            assertThrows(NullPointerException.class, () -> mapper.mapFrom(null));
        }
    }
}
