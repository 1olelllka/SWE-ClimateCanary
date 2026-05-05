package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.AbsenceCreateDTO;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.mappers.AbsenceCreateMapper;
import at.qe.skeleton.model.Absence;
import at.qe.skeleton.model.AbsenceType;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.UserxRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceCreateMapperUnitTests {

    @Mock
    private UserxRepository userxRepository;

    @InjectMocks
    private AbsenceCreateMapper mapper;

    @Test
    void testThatMapFromShouldReconstructEntityWithUserReference() {
        UUID userId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(5);

        AbsenceCreateDTO dto = new AbsenceCreateDTO(
                userId,
                start,
                end,
                AbsenceType.ILLNESS,
                "Feeling unwell",
                UUID.randomUUID()
        );

        Userx shellUser = new Userx();
        shellUser.setId(userId);
        when(userxRepository.getReferenceById(userId)).thenReturn(shellUser);

        Absence result = mapper.mapFrom(dto);

        assertNotNull(result);
        assertEquals("Feeling unwell", result.getComment());
        assertEquals(dto.reason(), result.getTypeOfAbsence());
        assertEquals(start, result.getStartDate());
        assertEquals(end, result.getEndDate());
        assertEquals(dto.assignedTo(), result.getAssignedTo());

        assertNotNull(result.getUser());
        assertEquals(userId, result.getUser().getId());

        verify(userxRepository, times(1)).getReferenceById(userId);
    }

    @Test
    void testThatMapFromShouldThrowNotFoundExceptionIfUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(5);

        AbsenceCreateDTO dto = new AbsenceCreateDTO(
                userId,
                start,
                end,
                AbsenceType.ILLNESS,
                "Feeling unwell",
                UUID.randomUUID()
        );

        Userx shellUser = new Userx();
        shellUser.setId(userId);
        when(userxRepository.getReferenceById(userId)).thenThrow(EntityNotFoundException.class);

        assertThrows(NotFoundException.class, () -> mapper.mapFrom(dto));

        verify(userxRepository, times(1)).getReferenceById(userId);
    }


    @Test
    void testThatMapToShouldThrowUnsupportedOperationException() {
        // Arrange
        Absence entity = new Absence();

        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> mapper.mapTo(entity));
    }
}