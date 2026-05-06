package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.model.SensorStation;
import org.springframework.stereotype.Component;

@Component
public class SensorStationMapper implements DTOMapper<SensorStation, SensorStationDTO> {

    @Override
    public SensorStationDTO mapTo(SensorStation entity) {
        return new SensorStationDTO(
                entity.getReadId(),
                entity.getWriteId(),
                entity.getName(),
                entity.getStatus(),
                entity.getRoomMonitoring() != null ? entity.getRoomMonitoring().getRoomId() : null,
                entity.getRoomMonitoring() != null && entity.getRoomMonitoring().getRaspberryPi() != null
                        ? entity.getRoomMonitoring().getRaspberryPi().getId() : null
        );
    }

    @Override
    public SensorStation mapFrom(SensorStationDTO dto) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
