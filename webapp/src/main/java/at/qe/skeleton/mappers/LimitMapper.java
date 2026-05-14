package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.LimitDTO;
import at.qe.skeleton.model.HumidityLimit;
import at.qe.skeleton.model.PollutionLimit;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.TemperatureLimit;
import org.springframework.stereotype.Component;

@Component
public class LimitMapper implements DTOMapper<RoomMonitoring, LimitDTO> {

    private static final Float DEFAULT_TEMP_MIN = 18f;
    private static final Float DEFAULT_TEMP_MAX = 26f;
    private static final Float DEFAULT_HUM_MIN  = 30f;
    private static final Float DEFAULT_HUM_MAX  = 70f;
    private static final Float DEFAULT_CO2_MAX  = 800f;

    @Override
    public LimitDTO mapTo(RoomMonitoring entity) {
        TemperatureLimit tempLimit = entity.getTempLimit();
        HumidityLimit humLimit = entity.getHumLimit();
        PollutionLimit polLimit = entity.getPolLimit();

        return new LimitDTO(
                entity.getRoomId(),
                valueOrDefault(tempLimit != null ? tempLimit.getMinVal() : null, DEFAULT_TEMP_MIN),
                valueOrDefault(tempLimit != null ? tempLimit.getMaxVal() : null, DEFAULT_TEMP_MAX),
                valueOrDefault(humLimit != null ? humLimit.getMinVal() : null, DEFAULT_HUM_MIN),
                valueOrDefault(humLimit != null ? humLimit.getMaxVal() : null, DEFAULT_HUM_MAX),
                valueOrDefault(polLimit != null ? polLimit.getMaxVal() : null, DEFAULT_CO2_MAX)
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

    private float valueOrDefault(Float value, float defaultValue) {
        return value != null ? value : defaultValue;
    }
}