package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.MeasurementBatchDTO;
import at.qe.skeleton.dtos.ReadingDTO;
import at.qe.skeleton.mappers.MeasurementBatchMapper;
import at.qe.skeleton.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("MeasurementBatchMapper")
class MeasurementBatchMapperUnitTests {

    private MeasurementBatchMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MeasurementBatchMapper();
    }

    private RoomMonitoring buildRoomMonitoring() {
        return RoomMonitoring.builder()
                .roomId(UUID.randomUUID())
                .build();
    }

    private MeasurementBatchDTO buildBatch(UUID roomId, LocalDateTime timestamp, List<ReadingDTO> readings) {
        return new MeasurementBatchDTO(roomId, timestamp, readings);
    }


    @Nested
    @DisplayName("mapFrom(MeasurementBatchDTO, RoomMonitoring)")
    class MapFrom {

        @Test
        @DisplayName("maps all three reading types correctly")
        void mapsAllReadingTypes() {
            LocalDateTime now = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), now, List.of(
                    new ReadingDTO(MeasurementType.TEMPERATURE, 22.5),
                    new ReadingDTO(MeasurementType.HUMIDITY, 55.3),
                    new ReadingDTO(MeasurementType.CO2, 400.7)
            ));

            ClimateStats result = mapper.mapFrom(dto, room);

            assertThat(result).isNotNull();
            assertThat(result.getDate()).isEqualTo(now);
            assertThat(result.getTempVal()).isEqualTo(22.5);
            assertThat(result.getHumVal()).isEqualTo(55.3);
            assertThat(result.getPollVal()).isEqualTo(400.7);
            assertThat(result.getRoomMonitoring()).isEqualTo(room);
        }

        @Test
        @DisplayName("preserves double precision — no truncation occurs")
        void preservesDoublePrecision() {
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), LocalDateTime.now(), List.of(
                    new ReadingDTO(MeasurementType.TEMPERATURE, 22.987654321),
                    new ReadingDTO(MeasurementType.HUMIDITY, 55.123456789)
            ));

            ClimateStats result = mapper.mapFrom(dto, room);

            assertThat(result.getTempVal()).isCloseTo(22.987654321, within(0.000000001));
            assertThat(result.getHumVal()).isCloseTo(55.123456789, within(0.000000001));
        }

        @Test
        @DisplayName("maps only TEMPERATURE reading — humidity and co2 default to zero")
        void mapsOnlyTemperature() {
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), LocalDateTime.now(), List.of(
                    new ReadingDTO(MeasurementType.TEMPERATURE, 22.5)
            ));

            ClimateStats result = mapper.mapFrom(dto, room);

            assertThat(result.getTempVal()).isEqualTo(22.5);
            assertThat(result.getHumVal()).isZero();
            assertThat(result.getPollVal()).isZero();
        }

        @Test
        @DisplayName("maps only HUMIDITY reading — temperature and co2 default to zero")
        void mapsOnlyHumidity() {
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), LocalDateTime.now(), List.of(
                    new ReadingDTO(MeasurementType.HUMIDITY, 55.3)
            ));

            ClimateStats result = mapper.mapFrom(dto, room);

            assertThat(result.getHumVal()).isEqualTo(55.3);
            assertThat(result.getTempVal()).isZero();
            assertThat(result.getPollVal()).isZero();
        }

        @Test
        @DisplayName("maps only CO2 reading — temperature and humidity default to zero")
        void mapsOnlyCo2() {
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), LocalDateTime.now(), List.of(
                    new ReadingDTO(MeasurementType.CO2, 400.7)
            ));

            ClimateStats result = mapper.mapFrom(dto, room);

            assertThat(result.getPollVal()).isEqualTo(400.7);
            assertThat(result.getTempVal()).isZero();
            assertThat(result.getHumVal()).isZero();
        }

        @Test
        @DisplayName("maps negative temperature correctly")
        void mapsNegativeTemperature() {
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), LocalDateTime.now(), List.of(
                    new ReadingDTO(MeasurementType.TEMPERATURE, -10.5)
            ));

            ClimateStats result = mapper.mapFrom(dto, room);

            assertThat(result.getTempVal()).isEqualTo(-10.5);
        }

        @Test
        @DisplayName("maps zero values correctly")
        void mapsZeroValues() {
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), LocalDateTime.now(), List.of(
                    new ReadingDTO(MeasurementType.TEMPERATURE, 0.0),
                    new ReadingDTO(MeasurementType.HUMIDITY, 0.0),
                    new ReadingDTO(MeasurementType.CO2, 0.0)
            ));

            ClimateStats result = mapper.mapFrom(dto, room);

            assertThat(result.getTempVal()).isZero();
            assertThat(result.getHumVal()).isZero();
            assertThat(result.getPollVal()).isZero();
        }

        @Test
        @DisplayName("last reading wins when same type appears multiple times")
        void lastReadingWinsForDuplicateType() {
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), LocalDateTime.now(), List.of(
                    new ReadingDTO(MeasurementType.TEMPERATURE, 20.0),
                    new ReadingDTO(MeasurementType.TEMPERATURE, 25.5)  // overrides first
            ));

            ClimateStats result = mapper.mapFrom(dto, room);

            assertThat(result.getTempVal()).isEqualTo(25.5);
        }

        @Test
        @DisplayName("sets roomMonitoring reference correctly")
        void setsRoomMonitoring() {
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), LocalDateTime.now(), List.of(
                    new ReadingDTO(MeasurementType.TEMPERATURE, 22.5)
            ));

            ClimateStats result = mapper.mapFrom(dto, room);

            assertThat(result.getRoomMonitoring()).isSameAs(room);
        }

        @Test
        @DisplayName("preserves exact timestamp with nanoseconds")
        void preservesExactTimestamp() {
            LocalDateTime preciseTime = LocalDateTime.of(2024, 6, 15, 14, 30, 45, 123456789);
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), preciseTime, List.of(
                    new ReadingDTO(MeasurementType.TEMPERATURE, 22.5)
            ));

            ClimateStats result = mapper.mapFrom(dto, room);

            assertThat(result.getDate()).isEqualTo(preciseTime);
        }

        @Test
        @DisplayName("does not set id — left for JPA generation")
        void doesNotSetId() {
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), LocalDateTime.now(), List.of(
                    new ReadingDTO(MeasurementType.TEMPERATURE, 22.5)
            ));

            ClimateStats result = mapper.mapFrom(dto, room);

            assertThat(result.getId()).isNull();
        }

        @Test
        @DisplayName("produces entity with all zero sensor values when readings list is empty")
        void emptyReadingsProducesZeroValues() {
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), LocalDateTime.now(), List.of());

            ClimateStats result = mapper.mapFrom(dto, room);

            assertThat(result.getTempVal()).isZero();
            assertThat(result.getHumVal()).isZero();
            assertThat(result.getPollVal()).isZero();
        }

        @Test
        @DisplayName("throws NullPointerException when readings list is null")
        void throwsOnNullReadingsList() {
            RoomMonitoring room = buildRoomMonitoring();
            MeasurementBatchDTO dto = buildBatch(room.getRoomId(), LocalDateTime.now(), null);

            assertThrows(NullPointerException.class, () -> mapper.mapFrom(dto, room));
        }

        @Test
        @DisplayName("null roomMonitoring is passed through — JPA will reject at persist time")
        void nullRoomMonitoringPassedThrough() {
            MeasurementBatchDTO dto = buildBatch(UUID.randomUUID(), LocalDateTime.now(), List.of(
                    new ReadingDTO(MeasurementType.TEMPERATURE, 22.5)
            ));

            ClimateStats result = mapper.mapFrom(dto, null);

            assertThat(result.getRoomMonitoring()).isNull();
        }
    }
}