package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.LimitDTO;
import at.qe.skeleton.model.HumidityLimit;
import at.qe.skeleton.model.PollutionLimit;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.TemperatureLimit;
import org.springframework.stereotype.Component;

@Component
public class LimitMapper implements DTOMapper<RoomMonitoring, LimitDTO> {

    @Override
    public LimitDTO mapTo(RoomMonitoring entity) {
        TemperatureLimit temp = entity.getTempLimit();
        HumidityLimit    hum  = entity.getHumLimit();
        PollutionLimit   pol  = entity.getPolLimit();
        return new LimitDTO(
                entity.getRoomId(),
                temp != null && temp.getMinVal() != null ? temp.getMinVal() : 18f,
                temp != null ? temp.getMaxVal() : 26f,
                hum  != null ? hum.getMaxVal()  : 70f,
                hum  != null && hum.getMinVal()  != null ? hum.getMinVal()  : 30f,
                pol  != null ? pol.getMaxVal()  : 800f
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