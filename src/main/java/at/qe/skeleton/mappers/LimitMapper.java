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
        return new LimitDTO(
                entity.getRoomId(),
                entity.getTempLimit().getMinVal(),
                entity.getTempLimit().getMaxVal(),
                entity.getHumLimit().getMinVal(),
                entity.getHumLimit().getMaxVal(),
                entity.getPolLimit().getMaxVal()
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