package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.RaspberryPatchDTO;
import at.qe.skeleton.model.RaspberryPi;
import org.springframework.stereotype.Component;

@Component
public class RaspberryPatchMapper implements DTOMapper<RaspberryPi, RaspberryPatchDTO> {
    @Override
    public RaspberryPatchDTO mapTo(RaspberryPi entity) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public RaspberryPi mapFrom(RaspberryPatchDTO dto) {
        return RaspberryPi
                .builder()
                .name(dto.name())
                .ip(dto.ipAddress())
                .frequency(dto.frequency())
                .build();
    }
}
