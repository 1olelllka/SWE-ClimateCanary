package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.RaspberryDTO;
import at.qe.skeleton.dtos.RoomRaspberry;
import at.qe.skeleton.model.RaspberryPi;
import org.springframework.stereotype.Component;

@Component
public class RaspberryMapper implements DTOMapper<RaspberryPi, RaspberryDTO> {

    @Override
    public RaspberryDTO mapTo(RaspberryPi entity) {
        RoomRaspberry room = entity.getRoomMonitoring() != null
                ? new RoomRaspberry(entity.getRoomMonitoring().getRoomId(), entity.getRoomMonitoring().getRoomNumber())
                : null;
        return new RaspberryDTO(
                entity.getId(),
                entity.getName(),
                entity.getIp(),
                entity.getPort(),
                entity.getFrequency(),
                entity.getStatus(),
                room);
    }

    @Override
    public RaspberryPi mapFrom(RaspberryDTO dto) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
