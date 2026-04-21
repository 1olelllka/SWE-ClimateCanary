package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.RaspberryDTO;
import at.qe.skeleton.dtos.RoomRaspberry;
import at.qe.skeleton.model.RaspberryPi;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

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
                entity.getRoomsMonitoring() != null ?
                        entity.getRoomsMonitoring().stream().map(r -> new RoomRaspberry(r.getRoomId(), r.getRoomNumber())).collect(Collectors.toSet())
                : Set.of());
    }

    @Override
    public RaspberryPi mapFrom(RaspberryDTO dto) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
