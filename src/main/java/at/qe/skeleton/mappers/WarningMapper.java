package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.Warnings;
import org.springframework.stereotype.Component;

@Component
public class WarningMapper implements DTOMapper<Warnings, WarningDTO> {

    @Override
    public WarningDTO mapTo(Warnings entity) {
        return new WarningDTO(
                entity.getId(),
                entity.getRoomMonitoring().getRoomId(),
                entity.getMeasurementType(),
                entity.getStatus(),
                entity.getMessage(),
                entity.getTriggeredValue(),
                entity.getActiveLimitAtTime(),
                entity.getCreatedAt(),
                entity.getResolvedAt(),
                entity.isActive()
        );
    }

    @Override
    public Warnings mapFrom(WarningDTO dto) {
        return Warnings.builder()
                .id(dto.id())
                .roomMonitoring(RoomMonitoring.builder()
                        .roomId(dto.roomId())
                        .build())
                .measurementType(dto.measurementType())
                .status(dto.status())
                .message(dto.message())
                .triggeredValue(dto.triggeredValue())
                .activeLimitAtTime(dto.activeLimitAtTime())
                .createdAt(dto.createdAt())
                .resolvedAt(dto.resolvedAt())
                .build();
    }
}
