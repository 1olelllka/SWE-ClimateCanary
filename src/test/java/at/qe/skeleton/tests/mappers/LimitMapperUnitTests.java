package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.LimitDTO;
import at.qe.skeleton.mappers.LimitMapper;
import at.qe.skeleton.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("LimitMapper")
class LimitMapperUnitTests {

    private LimitMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LimitMapper();
    }

    private RoomMonitoring buildRoomMonitoring(UUID roomId,
                                               float tempMin, float tempMax,
                                               float humMin, float humMax,
                                               int co2Max) {
        return RoomMonitoring.builder()
                .roomId(roomId)
                .tempLimit(TemperatureLimit.builder()
                        .minVal(tempMin)
                        .maxVal(tempMax)
                        .build())
                .humLimit(HumidityLimit.builder()
                        .minVal(humMin)
                        .maxVal(humMax)
                        .build())
                .polLimit(PollutionLimit.builder()
                        .maxVal(co2Max)
                        .build())
                .build();
    }

    @Nested
    @DisplayName("mapTo(RoomMonitoring) — Entity to DTO")
    class MapTo {

        @Test
        @DisplayName("maps all limit fields correctly")
        void mapsAllFields() {
            UUID roomId = UUID.randomUUID();
            RoomMonitoring entity = buildRoomMonitoring(roomId, 18.0f, 26.0f, 30.0f, 70.0f, 1000);

            LimitDTO result = mapper.mapTo(entity);

            assertThat(result).isNotNull();
            assertThat(result.roomId()).isEqualTo(roomId);
            assertThat(result.tempMin()).isEqualTo(18.0f);
            assertThat(result.tempMax()).isEqualTo(26.0f);
            assertThat(result.humMin()).isEqualTo(30.0f);
            assertThat(result.humMax()).isEqualTo(70.0f);
            assertThat(result.co2Max()).isEqualTo(1000);
        }

        @Test
        @DisplayName("maps zero limit values correctly")
        void mapsZeroValues() {
            UUID roomId = UUID.randomUUID();
            RoomMonitoring entity = buildRoomMonitoring(roomId, 0f, 0f, 0f, 0f, 0);

            LimitDTO result = mapper.mapTo(entity);

            assertThat(result.tempMin()).isZero();
            assertThat(result.tempMax()).isZero();
            assertThat(result.humMin()).isZero();
            assertThat(result.humMax()).isZero();
            assertThat(result.co2Max()).isZero();
        }

        @Test
        @DisplayName("maps negative temperature limits correctly")
        void mapsNegativeTemperatureLimits() {
            UUID roomId = UUID.randomUUID();
            RoomMonitoring entity = buildRoomMonitoring(roomId, -20.0f, -5.0f, 20.0f, 60.0f, 800);

            LimitDTO result = mapper.mapTo(entity);

            assertThat(result.tempMin()).isEqualTo(-20.0f);
            assertThat(result.tempMax()).isEqualTo(-5.0f);
        }

        @Test
        @DisplayName("maps maximum float values correctly")
        void mapsMaxFloatValues() {
            UUID roomId = UUID.randomUUID();
            RoomMonitoring entity = buildRoomMonitoring(
                    roomId, Float.MAX_VALUE, Float.MAX_VALUE,
                    Float.MAX_VALUE, Float.MAX_VALUE, Integer.MAX_VALUE);

            LimitDTO result = mapper.mapTo(entity);

            assertThat(result.tempMin()).isEqualTo(Float.MAX_VALUE);
            assertThat(result.tempMax()).isEqualTo(Float.MAX_VALUE);
            assertThat(result.humMin()).isEqualTo(Float.MAX_VALUE);
            assertThat(result.humMax()).isEqualTo(Float.MAX_VALUE);
            assertThat(result.co2Max()).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("throws NullPointerException when entity is null")
        void throwsOnNullEntity() {
            assertThrows(NullPointerException.class, () -> mapper.mapTo(null));
        }

        @Test
        @DisplayName("throws NullPointerException when tempLimit is null")
        void throwsWhenTempLimitIsNull() {
            RoomMonitoring entity = RoomMonitoring.builder()
                    .roomId(UUID.randomUUID())
                    .tempLimit(null)
                    .humLimit(HumidityLimit.builder().minVal(30f).maxVal(70f).build())
                    .polLimit(PollutionLimit.builder().maxVal(1000).build())
                    .build();

            assertThrows(NullPointerException.class, () -> mapper.mapTo(entity));
        }

        @Test
        @DisplayName("throws NullPointerException when humLimit is null")
        void throwsWhenHumLimitIsNull() {
            RoomMonitoring entity = RoomMonitoring.builder()
                    .roomId(UUID.randomUUID())
                    .tempLimit(TemperatureLimit.builder().minVal(18f).maxVal(26f).build())
                    .humLimit(null)
                    .polLimit(PollutionLimit.builder().maxVal(1000).build())
                    .build();

            assertThrows(NullPointerException.class, () -> mapper.mapTo(entity));
        }

        @Test
        @DisplayName("throws NullPointerException when polLimit is null")
        void throwsWhenPolLimitIsNull() {
            RoomMonitoring entity = RoomMonitoring.builder()
                    .roomId(UUID.randomUUID())
                    .tempLimit(TemperatureLimit.builder().minVal(18f).maxVal(26f).build())
                    .humLimit(HumidityLimit.builder().minVal(30f).maxVal(70f).build())
                    .polLimit(null)
                    .build();

            assertThrows(NullPointerException.class, () -> mapper.mapTo(entity));
        }
    }

    @Nested
    @DisplayName("mapFrom(LimitDTO) — DTO to Entity")
    class MapFrom {

        @Test
        @DisplayName("maps all fields correctly")
        void mapsAllFields() {
            UUID roomId = UUID.randomUUID();
            LimitDTO dto = new LimitDTO(roomId, 18.0f, 26.0f, 70.0f, 30.0f, 1000);

            RoomMonitoring result = mapper.mapFrom(dto);

            assertThat(result).isNotNull();
            assertThat(result.getRoomId()).isEqualTo(roomId);

            assertThat(result.getTempLimit()).isNotNull();
            assertThat(result.getTempLimit().getMinVal()).isEqualTo(18.0f);
            assertThat(result.getTempLimit().getMaxVal()).isEqualTo(26.0f);

            assertThat(result.getHumLimit()).isNotNull();
            assertThat(result.getHumLimit().getMinVal()).isEqualTo(30.0f);
            assertThat(result.getHumLimit().getMaxVal()).isEqualTo(70.0f);

            assertThat(result.getPolLimit()).isNotNull();
            assertThat(result.getPolLimit().getMaxVal()).isEqualTo(1000);
        }

        @Test
        @DisplayName("maps zero limit values correctly")
        void mapsZeroValues() {
            LimitDTO dto = new LimitDTO(UUID.randomUUID(), 0f, 0f, 0f, 0f, 0);

            RoomMonitoring result = mapper.mapFrom(dto);

            assertThat(result.getTempLimit().getMinVal()).isZero();
            assertThat(result.getTempLimit().getMaxVal()).isZero();
            assertThat(result.getHumLimit().getMinVal()).isZero();
            assertThat(result.getHumLimit().getMaxVal()).isZero();
            assertThat(result.getPolLimit().getMaxVal()).isZero();
        }

        @Test
        @DisplayName("maps negative temperature limits correctly")
        void mapsNegativeTemperatureLimits() {
            LimitDTO dto = new LimitDTO(UUID.randomUUID(), -20.0f, -5.0f, 60.0f, 20.0f, 800);

            RoomMonitoring result = mapper.mapFrom(dto);

            assertThat(result.getTempLimit().getMinVal()).isEqualTo(-20.0f);
            assertThat(result.getTempLimit().getMaxVal()).isEqualTo(-5.0f);
        }

        @Test
        @DisplayName("does not set devices — left for service layer")
        void doesNotSetDevices() {
            LimitDTO dto = new LimitDTO(UUID.randomUUID(), 18.0f, 26.0f, 70.0f, 30.0f, 1000);

            RoomMonitoring result = mapper.mapFrom(dto);

            assertThat(result.getRaspberryPi()).isNull();
            assertThat(result.getSensorStations()).isNullOrEmpty();
        }

        @Test
        @DisplayName("does not set climate stats — left for service layer")
        void doesNotSetClimateStats() {
            LimitDTO dto = new LimitDTO(UUID.randomUUID(), 18.0f, 26.0f, 70.0f, 30.0f, 1000);

            RoomMonitoring result = mapper.mapFrom(dto);

            assertThat(result.getClimateStats()).isEmpty();
        }

        @Test
        @DisplayName("does not set warnings — left for service layer")
        void doesNotSetWarnings() {
            LimitDTO dto = new LimitDTO(UUID.randomUUID(), 18.0f, 26.0f, 70.0f, 30.0f, 1000);

            RoomMonitoring result = mapper.mapFrom(dto);

            assertThat(result.getWarnings()).isEmpty();
        }

        @Test
        @DisplayName("throws NullPointerException when dto is null")
        void throwsOnNullDto() {
            assertThrows(NullPointerException.class, () -> mapper.mapFrom(null));
        }
    }

    @Nested
    @DisplayName("Round-trip")
    class RoundTrip {

        @Test
        @DisplayName("mapTo then mapFrom preserves all limit values")
        void entityToDtoToEntity() {
            UUID roomId = UUID.randomUUID();
            RoomMonitoring original = buildRoomMonitoring(roomId, 18.0f, 26.0f, 30.0f, 70.0f, 1000);

            RoomMonitoring result = mapper.mapFrom(mapper.mapTo(original));

            assertThat(result.getRoomId()).isEqualTo(original.getRoomId());
            assertThat(result.getTempLimit().getMinVal()).isEqualTo(original.getTempLimit().getMinVal());
            assertThat(result.getTempLimit().getMaxVal()).isEqualTo(original.getTempLimit().getMaxVal());
            assertThat(result.getHumLimit().getMinVal()).isEqualTo(original.getHumLimit().getMinVal());
            assertThat(result.getHumLimit().getMaxVal()).isEqualTo(original.getHumLimit().getMaxVal());
            assertThat(result.getPolLimit().getMaxVal()).isEqualTo(original.getPolLimit().getMaxVal());
        }

        @Test
        @DisplayName("mapFrom then mapTo preserves all limit values")
        void dtoToEntityToDto() {
            UUID roomId = UUID.randomUUID();
            LimitDTO original = new LimitDTO(roomId, 18.0f, 26.0f, 70.0f, 30.0f, 1000);

            LimitDTO result = mapper.mapTo(mapper.mapFrom(original));

            assertThat(result.roomId()).isEqualTo(original.roomId());
            assertThat(result.tempMin()).isEqualTo(original.tempMin());
            assertThat(result.tempMax()).isEqualTo(original.tempMax());
            assertThat(result.humMin()).isEqualTo(original.humMin());
            assertThat(result.humMax()).isEqualTo(original.humMax());
            assertThat(result.co2Max()).isEqualTo(original.co2Max());
        }
    }
}
