package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.ResourceNotFoundException;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RoomServiceImpl {
    private final RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository){
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllDepartments() {return roomRepository.findAll();}

    public Room getRoomById(UUID id){
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }

    public Room createRoom(Room room) {
        return roomRepository.save(room);
    }

    public void deleteRoom(UUID id) {
        roomRepository.deleteById(id);
    }

    public Department getDepartmentOfRoom(UUID roomId) {
        Room room = getRoomById(roomId);
        return room.getDepartment();
    }
}
