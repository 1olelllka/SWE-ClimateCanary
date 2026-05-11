package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.mappers.WarningCreateMapper;
import at.qe.skeleton.model.MeasurementType;
import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.model.Warnings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class WarningCreateMapperUnitTests {

    private WarningCreateMapper mapper = new WarningCreateMapper();

    @Test
    @DisplayName("map to DTO throws error")
    public void testThatMapToDTOThrowsError() {
        assertThrows(UnsupportedOperationException.class, () -> mapper.mapTo(null));
    }

    @Test
    @DisplayName("map from DTO works fine and resolved date is null")
    public void testThatMapFromDTOWorksFine() {
        UUID roomId = UUID.randomUUID();
        WarningCreateDTO dto = new WarningCreateDTO(roomId, "Test Device", MeasurementType.TEMPERATURE, WarningStatus.YELLOW,
                10.0, 20, "Msg", UUID.randomUUID());
        Warnings result = mapper.mapFrom(dto);
        assertAll(
                () -> assertEquals(dto.message(), result.getMessage()),
                () -> assertEquals(dto.device(), result.getDeviceName()),
                () -> assertEquals(dto.measurementType(), result.getMeasurementType()),
                () -> assertNull(result.getRoomMonitoring()),
                () -> assertEquals(dto.status(), result.getStatus()),
                () -> assertEquals(dto.triggeredValue(), result.getTriggeredValue()),
                () -> assertEquals(dto.activeLimitAtTime(), result.getActiveLimitAtTime()),
                () -> assertNull(result.getResolvedAt()),
                () -> assertNotNull(result.getCreatedAt())
        );
    }

}
