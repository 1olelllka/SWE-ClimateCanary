package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.AbsenceListDTO;
import at.qe.skeleton.mappers.AbsenceListMapper;
import at.qe.skeleton.model.*;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AbsenceListMapperUnitTests {

    private final AbsenceListMapper mapper = new AbsenceListMapper();

    @Test
    void testThatMapToShouldIncludeUserInfoAndRoomId() {
        // Arrange
        UUID absenceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        Room room = Room.builder().id(roomId).build();
        Userx user = TestDataUtil.createUserxEntity(null, room);
        user.setId(userId);

        Absence entity = Absence.builder()
                .id(absenceId)
                .user(user)
                .startDate(start)
                .endDate(end)
                .typeOfAbsence(AbsenceType.VACATION)
                .status(AbsenceStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        AbsenceListDTO result = mapper.mapTo(entity);

        assertEquals(absenceId, result.id());
        assertEquals(userId, result.userId());
        assertEquals("John", result.firstName());
        assertEquals(roomId, result.room());
        assertEquals(entity.getTypeOfAbsence(), result.typeOfAbsence());
    }

    @Test
    void testThatMapToWhenUserOrRoomIsNullShouldHandleNullsSafely() {
        Absence entity = Absence.builder()
                .id(UUID.randomUUID())
                .user(null)
                .build();

        AbsenceListDTO result = mapper.mapTo(entity);

        assertNull(result.userId());
        assertNull(result.firstName());
        assertNull(result.room());

        entity.setUser(Userx.builder().id(UUID.randomUUID()).myRoom(null).build());
        AbsenceListDTO resultWithUserNoRoom = mapper.mapTo(entity);
        assertNotNull(resultWithUserNoRoom.userId());
        assertNull(resultWithUserNoRoom.room());
    }

    @Test
    void testThatMapFromShouldReconstructEntityWithUserShell() {
        // Arrange
        UUID absenceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        AbsenceListDTO dto = new AbsenceListDTO(
                absenceId,
                userId,
                "John",
                "Doe",
                UUID.randomUUID(),
                now,
                now.plusDays(1),
                AbsenceType.ILLNESS,
                AbsenceStatus.APPROVED,
                now
        );

        // Act
        Absence result = mapper.mapFrom(dto);

        // Assert
        assertEquals(absenceId, result.getId());
        assertNotNull(result.getUser());
        assertEquals(userId, result.getUser().getId());
        assertEquals(AbsenceStatus.APPROVED, result.getStatus());
        assertEquals(dto.typeOfAbsence(), result.getTypeOfAbsence());
    }
}