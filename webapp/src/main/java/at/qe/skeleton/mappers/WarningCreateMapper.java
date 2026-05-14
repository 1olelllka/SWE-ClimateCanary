package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.model.Warnings;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class WarningCreateMapper implements DTOMapper<Warnings, WarningCreateDTO> {

    @Override
    public WarningCreateDTO mapTo(Warnings entity) {
        throw new UnsupportedOperationException("Operation is not supported.");
    }

    @Override
    public Warnings mapFrom(WarningCreateDTO dto) {
        return Warnings.builder()
                .measurementType(dto.measurementType())
                .status(dto.status())
                .message(dto.message())
                .triggeredValue(dto.triggeredValue())
                .activeLimitAtTime(dto.activeLimitAtTime())
                .createdAt(LocalDateTime.now())
                .resolvedAt(null)
                .sensorWriteId(dto.sensorWriteId())
                .build();
    }
}
