package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.mappers.WarningMapper;
import at.qe.skeleton.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class WarningMapperUnitTests {

    private final WarningMapper mapper = new WarningMapper();

    private final UUID id = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();
    private final LocalDateTime createdAt = LocalDateTime.of(2024, 6, 15, 12, 0);
    private final LocalDateTime resolvedAt = createdAt.plusHours(2);

    private Warnings activeWarning() {
        RoomMonitoring room = RoomMonitoring.builder().roomId(roomId).build();
        return Warnings.builder()
                .id(id)
                .roomMonitoring(room)
                .deviceName("Sensor-01")
                .measurementType(MeasurementType.TEMPERATURE)
                .status(WarningStatus.YELLOW)
                .message("Too hot")
                .triggeredValue(28.5)
                .activeLimitAtTime(25.0)
                .createdAt(createdAt)
                .resolvedAt(null)
                .build();
    }

    @Test
    @DisplayName("mapTo maps all fields from entity to DTO correctly")
    void testThatMapToMapsAllFields() {
        Warnings entity = activeWarning();

        WarningDTO result = mapper.mapTo(entity);

        assertAll(
                () -> assertEquals(entity.getId(),                          result.id()),
                () -> assertEquals(entity.getRoomMonitoring().getRoomId(),  result.roomId()),
                () -> assertEquals(entity.getDeviceName(),                  result.deviceName()),
                () -> assertEquals(entity.getMeasurementType(),             result.measurementType()),
                () -> assertEquals(entity.getStatus(),                      result.status()),
                () -> assertEquals(entity.getMessage(),                     result.message()),
                () -> assertEquals(entity.getTriggeredValue(),              result.triggeredValue()),
                () -> assertEquals(entity.getActiveLimitAtTime(),           result.activeLimitAtTime()),
                () -> assertEquals(entity.getCreatedAt(),                   result.createdAt()),
                () -> assertNull(result.resolvedAt()),
                () -> assertTrue(result.active())
        );
    }

    @Test
    @DisplayName("mapTo sets active=false and resolvedAt when warning is resolved")
    void testThatMapToReflectsResolvedState() {
        Warnings entity = activeWarning().toBuilder()
                .resolvedAt(resolvedAt)
                .status(WarningStatus.GREEN)
                .build();

        WarningDTO result = mapper.mapTo(entity);

        assertAll(
                () -> assertEquals(resolvedAt,        result.resolvedAt()),
                () -> assertEquals(WarningStatus.GREEN, result.status()),
                () -> assertFalse(result.active())
        );
    }

    @Test
    @DisplayName("mapFrom maps all fields from DTO to entity correctly")
    void testThatMapFromMapsAllFields() {
        WarningDTO dto = new WarningDTO(
                id, roomId, "Sensor-01", MeasurementType.TEMPERATURE,
                WarningStatus.YELLOW, "Too hot", 28.5, 25.0,
                createdAt, null, true);

        Warnings result = mapper.mapFrom(dto);

        assertAll(
                () -> assertEquals(dto.id(),               result.getId()),
                () -> assertEquals(dto.roomId(),           result.getRoomMonitoring().getRoomId()),
                () -> assertEquals(dto.deviceName(),       result.getDeviceName()),
                () -> assertEquals(dto.measurementType(),  result.getMeasurementType()),
                () -> assertEquals(dto.status(),           result.getStatus()),
                () -> assertEquals(dto.message(),          result.getMessage()),
                () -> assertEquals(dto.triggeredValue(),   result.getTriggeredValue()),
                () -> assertEquals(dto.activeLimitAtTime(), result.getActiveLimitAtTime()),
                () -> assertEquals(dto.createdAt(),        result.getCreatedAt()),
                () -> assertNull(result.getResolvedAt())
        );
    }

    @Test
    @DisplayName("mapFrom preserves resolvedAt when DTO has it set")
    void testThatMapFromPreservesResolvedAt() {
        WarningDTO dto = new WarningDTO(
                id, roomId, "Sensor-01", MeasurementType.TEMPERATURE,
                WarningStatus.GREEN, "Resolved", 28.5, 25.0,
                createdAt, resolvedAt, false);

        Warnings result = mapper.mapFrom(dto);

        assertEquals(resolvedAt, result.getResolvedAt());
    }
}