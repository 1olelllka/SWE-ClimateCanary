package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.SensorStationPatchDTO;
import at.qe.skeleton.model.SensorStation;
import org.springframework.stereotype.Component;

@Component
public class SensorStationPatchMapper implements DTOMapper<SensorStation, SensorStationPatchDTO> {
    @Override
    public SensorStationPatchDTO mapTo(SensorStation entity) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public SensorStation mapFrom(SensorStationPatchDTO dto) {
        return SensorStation.builder()
                .name(dto.name())
                .status(dto.status())
                .lastHeartBeat(dto.lastHeartBeat())
                .build();
    }
}
