package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.RaspberryPatchDTO;
import at.qe.skeleton.mappers.RaspberryPatchMapper;
import at.qe.skeleton.model.RaspberryPi;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RaspberryPatchMapperUnitTests {

    private final RaspberryPatchMapper mapper = new RaspberryPatchMapper();

    @Test
    void testThatMapFromShouldReconstructEntityWithPatchData() {
        RaspberryPatchDTO dto = new RaspberryPatchDTO(
                "Pi-Kitchen",
                "192.168.1.15",
                5000,
                1000
        );

        RaspberryPi result = mapper.mapFrom(dto);

        assertNotNull(result);
        assertEquals("Pi-Kitchen", result.getName());
        assertEquals("192.168.1.15", result.getIp());
        assertEquals(5000, result.getFrequency());
    }

    @Test
    void testThatMapFromWhenDtoHasNullsShouldPreserveNullsInEntity() {
        RaspberryPatchDTO dto = new RaspberryPatchDTO(null, null, 0, 1000);

        RaspberryPi result = mapper.mapFrom(dto);

        assertNotNull(result);
        assertNull(result.getName());
        assertNull(result.getIp());
        assertEquals(0, result.getFrequency());
    }

    @Test
    void testThatMapToShouldThrowUnsupportedOperationException() {
        RaspberryPi entity = new RaspberryPi();

        assertThrows(UnsupportedOperationException.class, () -> mapper.mapTo(entity));
    }
}