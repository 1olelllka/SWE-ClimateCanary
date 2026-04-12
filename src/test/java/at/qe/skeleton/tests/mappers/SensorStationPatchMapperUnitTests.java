package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.SensorStationPatchDTO;
import at.qe.skeleton.mappers.SensorStationPatchMapper;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.SensorStation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SensorStationPatchMapperUnitTests {

    private final SensorStationPatchMapper mapper = new SensorStationPatchMapper();

    @Test
    void testThatMapFromShouldReconstructEntityWithUpdateData() {
        LocalDateTime now = LocalDateTime.now();
        SensorStationPatchDTO dto = new SensorStationPatchDTO(
                "Updated-Station-Name",
                now,
                DeviceStatus.ONLINE,
                null
        );

        SensorStation result = mapper.mapFrom(dto);

        assertNotNull(result);
        assertEquals("Updated-Station-Name", result.getName());
        assertEquals(DeviceStatus.ONLINE, result.getStatus());
        assertEquals(now, result.getLastHeartBeat());
    }

    @Test
    void testThatMapFromWhenDtoHasNullsShouldPreserveNullsInEntity() {
        SensorStationPatchDTO dto = new SensorStationPatchDTO(null, null, null, null);

        SensorStation result = mapper.mapFrom(dto);

        assertNotNull(result);
        assertNull(result.getName());
        assertNull(result.getStatus());
        assertNull(result.getLastHeartBeat());
    }

    @Test
    void testThatMapToShouldThrowUnsupportedOperationException() {
        SensorStation entity = new SensorStation();

        assertThrows(UnsupportedOperationException.class, () -> mapper.mapTo(entity));
    }
}