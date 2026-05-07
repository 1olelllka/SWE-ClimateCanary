package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.Tip;
import at.qe.skeleton.model.Warnings;
import org.springframework.stereotype.Component;

@Component
public class WarningMapper implements DTOMapper<Warnings, WarningDTO> {

    @Override
    public WarningDTO mapTo(Warnings entity) {
        return new WarningDTO(
                entity.getId(),
                entity.getRoomMonitoring().getRoomId(),
                entity.getDeviceName(),
                entity.getMeasurementType(),
                entity.getStatus(),
                entity.getMessage(),
                entity.getTriggeredValue(),
                entity.getActiveLimitAtTime(),
                entity.getCreatedAt(),
                entity.getResolvedAt(),
                entity.getTip() != null ? entity.getTip().getMsg() : "There's no tip.",
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
                .deviceName(dto.deviceName())
                .measurementType(dto.measurementType())
                .status(dto.status())
                .message(dto.message())
                .tip(Tip.builder().msg(dto.tip()).build())
                .triggeredValue(dto.triggeredValue())
                .activeLimitAtTime(dto.activeLimitAtTime())
                .createdAt(dto.createdAt())
                .resolvedAt(dto.resolvedAt())
                .build();
    }
}
