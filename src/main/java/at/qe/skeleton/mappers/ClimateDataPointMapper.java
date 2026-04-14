package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.ClimateDataPointDTO;
import at.qe.skeleton.model.ClimateStats;
import org.springframework.stereotype.Component;

@Component
public class ClimateDataPointMapper implements DTOMapper<ClimateStats, ClimateDataPointDTO> {

    @Override
    public ClimateDataPointDTO mapTo(ClimateStats entity) {
        return new ClimateDataPointDTO(
                entity.getDate(),
                entity.getTempVal(),
                entity.getHumVal(),
                entity.getPollVal()
        );
    }

    @Override
    public ClimateStats mapFrom(ClimateDataPointDTO dto) {
        return ClimateStats.builder()
                .date(dto.timestamp())
                .tempVal(dto.temperature())
                .humVal(dto.humidity())
                .pollVal(dto.airQuality())
                .build();
    }
}
