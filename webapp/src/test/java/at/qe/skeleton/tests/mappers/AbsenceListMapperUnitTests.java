package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.AbsenceListDTO;
import at.qe.skeleton.mappers.AbsenceListMapper;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbsenceListMapperUnitTests {

    @Mock
    private UserxRepository userxRepository;

    private AbsenceListMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AbsenceListMapper(userxRepository);
    }

    @Test
    void testThatMapToMapsUserInfoRoomNumberAndManagerName() {
        UUID absenceId = UUID.randomUUID();
        UUID userId    = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end   = LocalDateTime.now().plusDays(2);

        Room room = Room.builder().id(UUID.randomUUID()).roomNumber("ENG-101").build();
        Userx user = TestDataUtil.createUserxEntity(null, room);
        user.setId(userId);

        Userx manager = TestDataUtil.createUserxEntity(null, null);
        manager.setId(managerId);
        manager.setFirstName("Alice");
        manager.setLastName("Smith");

        Absence entity = Absence.builder()
                .id(absenceId)
                .user(user)
                .startDate(start)
                .endDate(end)
                .typeOfAbsence(AbsenceType.VACATION)
                .status(AbsenceStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .assignedTo(managerId)
                .comment("Test comment")
                .build();

        when(userxRepository.findById(managerId)).thenReturn(Optional.of(manager));

        AbsenceListDTO result = mapper.mapTo(entity);

        assertEquals(absenceId,   result.id());
        assertEquals(userId,      result.userId());
        assertEquals("John",      result.firstName());
        assertEquals("ENG-101",   result.roomNumber());
        assertEquals(AbsenceType.VACATION, result.typeOfAbsence());
        assertEquals("Test comment", result.comment());
        assertEquals("Alice",     result.managerFirstName());
        assertEquals("Smith",     result.managerLastName());
    }

    @Test
    void testThatMapToWhenUserOrRoomIsNullHandlesNullsSafely() {
        Absence entityNoUser = Absence.builder()
                .id(UUID.randomUUID())
                .user(null)
                .build();

        AbsenceListDTO result = mapper.mapTo(entityNoUser);

        assertNull(result.userId());
        assertNull(result.firstName());
        assertNull(result.roomNumber());
        assertNull(result.managerFirstName());
        assertNull(result.managerLastName());

        Absence entityNoRoom = Absence.builder()
                .id(UUID.randomUUID())
                .user(Userx.builder().id(UUID.randomUUID()).myRoom(null).build())
                .build();

        AbsenceListDTO resultWithUserNoRoom = mapper.mapTo(entityNoRoom);

        assertNotNull(resultWithUserNoRoom.userId());
        assertNull(resultWithUserNoRoom.roomNumber());
    }

    @Test
    void testThatMapToReturnsNullManagerNamesWhenManagerNotFound() {
        UUID managerId = UUID.randomUUID();
        Absence entity = Absence.builder()
                .id(UUID.randomUUID())
                .user(null)
                .assignedTo(managerId)
                .build();

        when(userxRepository.findById(managerId)).thenReturn(Optional.empty());

        AbsenceListDTO result = mapper.mapTo(entity);

        assertNull(result.managerFirstName());
        assertNull(result.managerLastName());
    }

    @Test
    void testThatMapToDoesNotQueryRepositoryWhenAssignedToIsNull() {
        Absence entity = Absence.builder()
                .id(UUID.randomUUID())
                .user(null)
                .assignedTo(null)
                .build();

        mapper.mapTo(entity);

        verifyNoInteractions(userxRepository);
    }

    @Test
    void testThatMapFromReconstructsEntityWithUserShell() {
        UUID absenceId = UUID.randomUUID();
        UUID userId    = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        AbsenceListDTO dto = new AbsenceListDTO(
                absenceId, userId,
                "John", "Doe", "ENG-101",
                now, now.plusDays(1),
                AbsenceType.ILLNESS, AbsenceStatus.APPROVED, now,
                "Some comment", "Alice", "Smith"
        );

        Absence result = mapper.mapFrom(dto);

        assertEquals(absenceId, result.getId());
        assertNotNull(result.getUser());
        assertEquals(userId, result.getUser().getId());
        assertEquals(AbsenceStatus.APPROVED, result.getStatus());
        assertEquals(AbsenceType.ILLNESS, result.getTypeOfAbsence());
    }
}
