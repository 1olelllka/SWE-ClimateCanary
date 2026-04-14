package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.Warnings;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class WarningCreateMapper implements DTOMapper<Warnings, WarningCreateDTO> {

    @Override
    public WarningCreateDTO mapTo(Warnings entity) {
        return new WarningCreateDTO(
                entity.getRoomMonitoring().getRoomId(),
                entity.getMeasurementType(),
                entity.getStatus(),
                entity.getTriggeredValue(),
                entity.getActiveLimitAtTime(),
                entity.getMessage()
        );
    }

    @Override
    public Warnings mapFrom(WarningCreateDTO dto) {
        return Warnings.builder()
                .roomMonitoring(RoomMonitoring.builder()
                        .roomId(dto.roomId())
                        .build())
                .measurementType(dto.measurementType())
                .status(dto.status())
                .message(dto.message())
                .triggeredValue(dto.triggeredValue())
                .activeLimitAtTime(dto.activeLimitAtTime())
                .createdAt(LocalDateTime.now())
                .resolvedAt(null)
                .build();
    }
}
