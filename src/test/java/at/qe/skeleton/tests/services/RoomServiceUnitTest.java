package at.qe.skeleton.tests.services;

import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.services.impl.RoomServiceImpl;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceUnitTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMonitoringRepository monitoringRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room sampleRoom;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();

        Building building = TestDataUtil.createBuildingEntity();
        Department department = TestDataUtil.createDepartmentEntity(building);

        // Use TestDataUtil to maintain consistency
        sampleRoom = TestDataUtil.createRoomEntity(department);
        sampleRoom.setId(roomId);
    }

    @Test
    void testThatGetPageOfRoomsShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> page = new PageImpl<>(List.of(sampleRoom));
        when(roomRepository.findAll(pageable)).thenReturn(page);

        Page<Room> result = roomService.getPageOfRooms(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(roomRepository).findAll(pageable);
    }

    @Test
    void testThatCreateRoomSuccessful() {
        when(roomRepository.save(any())).thenReturn(sampleRoom);

        Room result = roomService.createRoom(sampleRoom);
        RoomMonitoring monitoring = RoomMonitoring.builder().roomId(result.getId()).roomNumber(result.getRoomNumber()).build();

        assertNotNull(result);
        assertEquals(RoomType.OFFICE, result.getRoomType());
        assertEquals(10, result.getDefaultPeopleCnt());
        verify(roomRepository).save(sampleRoom);
        verify(monitoringRepository).save(monitoring);
    }

    @Test
    void testThatPatchRoomUpdatesFields() {
        // Create a "patch" object with only the fields we want to change
        Room patchData = Room.builder()
                .roomType(RoomType.OFFICE)
                .defaultPeopleCnt(5)
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Room result = roomService.patchRoom(roomId, patchData);

        assertEquals(RoomType.OFFICE, result.getRoomType());
        assertEquals(5, result.getDefaultPeopleCnt());
        assertTrue(result.getIsActive()); // Should remain true from TestDataUtil default
        verify(roomRepository).save(any());
    }

    @Test
    void testThatPatchRoomShouldThrowNotFound() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                roomService.patchRoom(roomId, Room.builder().build()));
    }

    @Test
    void testThatDeleteRoomShouldCallRepository() {
        roomService.deleteRoom(roomId);
        verify(roomRepository, times(1)).deleteById(roomId);
    }
}