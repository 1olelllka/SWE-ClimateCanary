package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.AggregatedDataPointDTO;
import at.qe.skeleton.model.AggregatedStats;
import org.springframework.stereotype.Component;

@Component
public class AggregatedStatsMapper implements DTOMapper<AggregatedStats, AggregatedDataPointDTO> {

    @Override
    public AggregatedDataPointDTO mapTo(AggregatedStats entity) {
        return new AggregatedDataPointDTO(
                entity.getDate(),
                entity.getAvgTemp(),
                entity.getAvgHumidity(),
                entity.getAvgCO2()
        );
    }

    @Override
    public AggregatedStats mapFrom(AggregatedDataPointDTO dto) {
        return AggregatedStats.builder()
                .date(dto.date())
                .avgTemp((float) dto.avgTemperature())
                .avgHumidity((float) dto.avgHumidity())
                .avgCO2((float) dto.avgAirQuality())
                .build();
    }
}
