package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.AbsenceDTO;
import at.qe.skeleton.mappers.AbsenceMapper;
import at.qe.skeleton.model.Absence;
import at.qe.skeleton.model.AbsenceStatus;
import at.qe.skeleton.model.AbsenceType;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AbsenceMapperUnitTests {

    private final AbsenceMapper mapper = new AbsenceMapper();

    @Test
    void testThatMapToShouldIncludeDetailFields() {
        Absence entity = TestDataUtil.createAbsence(null);
        UUID id = UUID.randomUUID();
        entity.setId(id);
        entity.setAssignedTo(UUID.randomUUID());

        AbsenceDTO result = mapper.mapTo(entity);

        assertNotNull(result);
        assertEquals(id, result.id());
        assertEquals(AbsenceType.ILLNESS, result.typeOfAbsense());
        assertEquals(AbsenceStatus.APPROVED, result.status());
        assertEquals("Simple test comment", result.comment());
        assertEquals(entity.getAssignedTo(), result.assignedTo());
        assertNotNull(result.startDate());
        assertNotNull(result.endDate());
    }

    @Test
    void testThatMapFromShouldThrowUnsupportedOperationException() {
        AbsenceDTO dto = new AbsenceDTO(
                UUID.randomUUID(),
                AbsenceType.VACATION,
                AbsenceStatus.PENDING,
                null, null, null,
                UUID.randomUUID(),
                "Comment"
        );

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> mapper.mapFrom(dto)
        );
        assertEquals("Function is not available.", exception.getMessage());
    }
}