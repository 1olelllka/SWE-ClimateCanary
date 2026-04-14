package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.RaspberryDTO;
import at.qe.skeleton.model.RaspberryPi;
import org.springframework.stereotype.Component;

@Component
public class RaspberryMapper implements DTOMapper<RaspberryPi, RaspberryDTO> {

    @Override
    public RaspberryDTO mapTo(RaspberryPi entity) {
        return new RaspberryDTO(
                entity.getId(),
                entity.getName(),
                entity.getIp(),
                entity.getPort(),
                entity.getStatus(),
                entity.getRoomMonitoring() != null ? entity.getRoomMonitoring().getRoomId() : null,
                entity.getRoomMonitoring() != null ? entity.getRoomMonitoring().getRoomNumber() : null
        );
    }

    @Override
    public RaspberryPi mapFrom(RaspberryDTO dto) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
