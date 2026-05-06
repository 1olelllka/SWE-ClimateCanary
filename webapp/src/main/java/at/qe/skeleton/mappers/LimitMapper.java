package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.LimitDTO;
import at.qe.skeleton.model.HumidityLimit;
import at.qe.skeleton.model.PollutionLimit;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.TemperatureLimit;
import org.springframework.stereotype.Component;

@Component
public class LimitMapper implements DTOMapper<RoomMonitoring, LimitDTO> {

    private static final float DEFAULT_TEMP_MIN = 18f;
    private static final float DEFAULT_TEMP_MAX = 26f;
    private static final float DEFAULT_HUM_MIN  = 30f;
    private static final float DEFAULT_HUM_MAX  = 70f;
    private static final float DEFAULT_CO2_MAX  = 800f;

    @Override
    public LimitDTO mapTo(RoomMonitoring entity) {
        return new LimitDTO(
                entity.getRoomId(),
                entity.getTempLimit() != null && entity.getTempLimit().getMinVal() != null? entity.getTempLimit().getMinVal() : DEFAULT_TEMP_MIN,
                entity.getTempLimit() != null ? entity.getTempLimit().getMaxVal() : DEFAULT_TEMP_MAX,
                entity.getHumLimit()  != null && entity.getHumLimit().getMinVal() != null? entity.getHumLimit().getMinVal()  : DEFAULT_HUM_MIN,
                entity.getHumLimit()  != null ? entity.getHumLimit().getMaxVal()  : DEFAULT_HUM_MAX,
                entity.getPolLimit()  != null ? entity.getPolLimit().getMaxVal()  : DEFAULT_CO2_MAX
        );
    }

    @Override
    public RoomMonitoring mapFrom(LimitDTO dto) {
        return RoomMonitoring.builder()
                .roomId(dto.roomId())
                .tempLimit(TemperatureLimit.builder()
                        .minVal(dto.tempMin())
                        .maxVal(dto.tempMax())
                        .build())
                .humLimit(HumidityLimit.builder()
                        .minVal(dto.humMin())
                        .maxVal(dto.humMax())
                        .build())
                .polLimit(PollutionLimit.builder()
                        .maxVal(dto.co2Max())
                        .build())
                .build();
    }
}