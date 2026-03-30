package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ResourceNotFoundException;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.services.RoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository){
        this.roomRepository = roomRepository;
    }

    public Page<Room> getPageOfRooms(Pageable pageable) {return roomRepository.findAll(pageable);}

    public Room getRoomById(UUID id){
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }

    public Room createRoom(Room room) {
        return roomRepository.save(room);
    }

    @Override
    public Room patchRoom(UUID id, Room room) {
        return roomRepository.findById(id).map(r -> {
            Optional.ofNullable(room.getRoomType()).ifPresent(r::setRoomType);
            Optional.ofNullable(room.getDepartment()).ifPresent(r::setDepartment);
            Optional.of(room.getDefaultPeopleCnt()).ifPresent(r::setDefaultPeopleCnt);
            return roomRepository.save(r);
        }).orElseThrow(() -> new NotFoundException("Room not found with id: " + id));
    }

    public void deleteRoom(UUID id) {
        roomRepository.deleteById(id);
    }

    public Department getDepartmentOfRoom(UUID roomId) {
        Room room = getRoomById(roomId);
        return room.getDepartment();
    }
}
