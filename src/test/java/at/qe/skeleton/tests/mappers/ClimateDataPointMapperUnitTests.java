package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.ClimateDataPointDTO;
import at.qe.skeleton.mappers.ClimateDataPointMapper;
import at.qe.skeleton.model.ClimateStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@DisplayName("ClimateDataPointMapper")
class ClimateDataPointMapperUnitTests {

    private ClimateDataPointMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ClimateDataPointMapper();
    }

    @Nested
    @DisplayName("mapTo(ClimateStats) — Entity to DTO")
    class MapTo {

        @Test
        @DisplayName("maps all fields correctly")
        void mapsAllFields() {
            LocalDateTime now = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            ClimateStats entity = ClimateStats.builder()
                    .date(now)
                    .tempVal(22)
                    .humVal(55)
                    .pollVal(400)
                    .build();

            ClimateDataPointDTO result = mapper.mapTo(entity);

            assertThat(result).isNotNull();
            assertThat(result.timestamp()).isEqualTo(now);
            assertThat(result.temperature()).isEqualTo(22);
            assertThat(result.humidity()).isEqualTo(55);
            assertThat(result.airQuality()).isEqualTo(400);
        }

        @Test
        @DisplayName("maps zero values correctly")
        void mapsZeroValues() {
            ClimateStats entity = ClimateStats.builder()
                    .date(LocalDateTime.of(2024, 1, 1, 0, 0))
                    .tempVal(0)
                    .humVal(0)
                    .pollVal(0)
                    .build();

            ClimateDataPointDTO result = mapper.mapTo(entity);

            assertThat(result.temperature()).isZero();
            assertThat(result.humidity()).isZero();
            assertThat(result.airQuality()).isZero();
        }

        @Test
        @DisplayName("maps negative temperature correctly")
        void mapsNegativeTemperature() {
            ClimateStats entity = ClimateStats.builder()
                    .date(LocalDateTime.of(2024, 1, 1, 0, 0))
                    .tempVal(-15)
                    .humVal(40)
                    .pollVal(200)
                    .build();

            ClimateDataPointDTO result = mapper.mapTo(entity);

            assertThat(result.temperature()).isEqualTo(-15);
        }

        @Test
        @DisplayName("maps maximum int values correctly")
        void mapsMaxValues() {
            ClimateStats entity = ClimateStats.builder()
                    .date(LocalDateTime.of(2024, 1, 1, 0, 0))
                    .tempVal(Integer.MAX_VALUE)
                    .humVal(Integer.MAX_VALUE)
                    .pollVal(Integer.MAX_VALUE)
                    .build();

            ClimateDataPointDTO result = mapper.mapTo(entity);

            assertThat(result.temperature()).isEqualTo(Integer.MAX_VALUE);
            assertThat(result.humidity()).isEqualTo(Integer.MAX_VALUE);
            assertThat(result.airQuality()).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("preserves exact timestamp with nanoseconds")
        void preservesExactTimestamp() {
            LocalDateTime preciseTime = LocalDateTime.of(2024, 6, 15, 14, 30, 45, 123456789);
            ClimateStats entity = ClimateStats.builder()
                    .date(preciseTime)
                    .tempVal(20)
                    .humVal(50)
                    .pollVal(300)
                    .build();

            ClimateDataPointDTO result = mapper.mapTo(entity);

            assertThat(result.timestamp()).isEqualTo(preciseTime);
        }

        @Test
        @DisplayName("throws NullPointerException when entity is null")
        void throwsOnNullEntity() {
            assertThrows(NullPointerException.class, () -> mapper.mapTo(null));
        }
    }

    @Nested
    @DisplayName("mapFrom(ClimateDataPointDTO) — DTO to Entity")
    class MapFrom {

        @Test
        @DisplayName("maps all fields correctly")
        void mapsAllFields() {
            LocalDateTime now = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            ClimateDataPointDTO dto = new ClimateDataPointDTO(now, 22.0, 55.0, 400);

            ClimateStats result = mapper.mapFrom(dto);

            assertThat(result).isNotNull();
            assertThat(result.getDate()).isEqualTo(now);
            assertThat(result.getTempVal()).isEqualTo(22);
            assertThat(result.getHumVal()).isEqualTo(55);
            assertThat(result.getPollVal()).isEqualTo(400);
        }

        @Test
        @DisplayName("truncates double temperature — does not round")
        void truncatesTemperatureNotRounds() {
            ClimateDataPointDTO dto = new ClimateDataPointDTO(
                    LocalDateTime.of(2024, 1, 1, 0, 0), 22.9, 50.0, 300);

            ClimateStats result = mapper.mapFrom(dto);

            // (int) cast truncates toward zero, 22.9 → 22, NOT 23
            assertThat(result.getTempVal()).isEqualTo(22);
        }

        @Test
        @DisplayName("truncates double humidity — does not round")
        void truncatesHumidityNotRounds() {
            ClimateDataPointDTO dto = new ClimateDataPointDTO(
                    LocalDateTime.of(2024, 1, 1, 0, 0), 20.0, 55.9, 300);

            ClimateStats result = mapper.mapFrom(dto);

            // (int) cast truncates toward zero, 55.9 → 55, NOT 56
            assertThat(result.getHumVal()).isEqualTo(55);
        }

        @Test
        @DisplayName("truncates negative double temperature correctly")
        void truncatesNegativeTemperature() {
            ClimateDataPointDTO dto = new ClimateDataPointDTO(
                    LocalDateTime.of(2024, 1, 1, 0, 0), -10.9, 50.0, 300);

            ClimateStats result = mapper.mapFrom(dto);

            // (int) cast truncates toward zero, -10.9 → -10, NOT -11
            assertThat(result.getTempVal()).isEqualTo(-10);
        }

        @Test
        @DisplayName("maps zero double values correctly")
        void mapsZeroValues() {
            ClimateDataPointDTO dto = new ClimateDataPointDTO(
                    LocalDateTime.of(2024, 1, 1, 0, 0), 0.0, 0.0, 0);

            ClimateStats result = mapper.mapFrom(dto);

            assertThat(result.getTempVal()).isZero();
            assertThat(result.getHumVal()).isZero();
            assertThat(result.getPollVal()).isZero();
        }

        @Test
        @DisplayName("does not set id — left for JPA generation")
        void doesNotSetId() {
            ClimateDataPointDTO dto = new ClimateDataPointDTO(
                    LocalDateTime.of(2024, 1, 1, 0, 0), 20.0, 50.0, 300);

            ClimateStats result = mapper.mapFrom(dto);

            assertThat(result.getId()).isNull();
        }

        @Test
        @DisplayName("does not set roomMonitoring — left for service layer")
        void doesNotSetRoomMonitoring() {
            ClimateDataPointDTO dto = new ClimateDataPointDTO(
                    LocalDateTime.of(2024, 1, 1, 0, 0), 20.0, 50.0, 300);

            ClimateStats result = mapper.mapFrom(dto);

            assertThat(result.getRoomMonitoring()).isNull();
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
        @DisplayName("mapTo then mapFrom preserves integer values losslessly")
        void entityToDtoToEntity() {
            LocalDateTime now = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            ClimateStats original = ClimateStats.builder()
                    .date(now)
                    .tempVal(22)
                    .humVal(55)
                    .pollVal(400)
                    .build();

            ClimateStats result = mapper.mapFrom(mapper.mapTo(original));

            assertThat(result.getDate()).isEqualTo(original.getDate());
            assertThat(result.getTempVal()).isEqualTo(original.getTempVal());
            assertThat(result.getHumVal()).isEqualTo(original.getHumVal());
            assertThat(result.getPollVal()).isEqualTo(original.getPollVal());
        }

        @Test
        @DisplayName("mapFrom then mapTo preserves exact double values that are whole numbers")
        void dtoToEntityToDto() {
            LocalDateTime now = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            ClimateDataPointDTO original = new ClimateDataPointDTO(now, 22.0, 55.0, 400);

            ClimateDataPointDTO result = mapper.mapTo(mapper.mapFrom(original));

            assertThat(result.timestamp()).isEqualTo(original.timestamp());
            assertThat(result.temperature()).isEqualTo(22.0);
            assertThat(result.humidity()).isEqualTo(55.0);
            assertThat(result.airQuality()).isEqualTo(original.airQuality());
        }
    }
}
