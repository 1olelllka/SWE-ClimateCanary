package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.AggregatedDataPointDTO;
import at.qe.skeleton.mappers.AggregatedStatsMapper;
import at.qe.skeleton.model.AggregatedStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AggregatedStatsMapperUnitTests {

    private AggregatedStatsMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AggregatedStatsMapper();
    }

    @Test
    void mapTo_ShouldSuccessfullyMapEntityToDto() {
        // Given
        LocalDate testDate = LocalDate.of(2026, 5, 16);
        AggregatedStats entity = AggregatedStats.builder()
                .date(testDate)
                .avgTemp(22.5f)
                .avgHumidity(45.0f)
                .avgCO2(415.8f)
                .build();

        // When
        AggregatedDataPointDTO dto = mapper.mapTo(entity);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.date()).isEqualTo(entity.getDate());
        assertThat(dto.avgTemperature()).isEqualTo(entity.getAvgTemp());
        assertThat(dto.avgHumidity()).isEqualTo(entity.getAvgHumidity());
        // Mapping maps avgCO2 to avgAirQuality
        assertThat(dto.avgAirQuality()).isEqualTo(entity.getAvgCO2());
    }

    @Test
    void mapFrom_ShouldSuccessfullyMapDtoToEntity() {
        // Given
        LocalDate testDate = LocalDate.of(2026, 5, 16);
        // Assuming DTO uses double/Double types based on your casts to float
        AggregatedDataPointDTO dto = new AggregatedDataPointDTO(testDate, 23.8, 50.2, 420.1);

        // When
        AggregatedStats entity = mapper.mapFrom(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getDate()).isEqualTo(dto.date());
        assertThat(entity.getAvgTemp()).isEqualTo((float) dto.avgTemperature());
        assertThat(entity.getAvgHumidity()).isEqualTo((float) dto.avgHumidity());
        assertThat(entity.getAvgCO2()).isEqualTo((float) dto.avgAirQuality());
    }

    @Test
    void mapTo_WithNullEntity_ShouldThrowNullPointerException() {
        // Verifies the current behavior if a null entity is passed
        assertThatThrownBy(() -> mapper.mapTo(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void mapFrom_WithNullDto_ShouldThrowNullPointerException() {
        // Verifies the current behavior if a null DTO is passed
        assertThatThrownBy(() -> mapper.mapFrom(null))
                .isInstanceOf(NullPointerException.class);
    }
}
