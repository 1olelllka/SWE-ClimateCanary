package at.qe.skeleton.services;

import at.qe.skeleton.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RoomService {
    Page<Room> getPageOfRooms(Pageable pageable);

    void deleteRoom(UUID id);

    Room createRoom(Room room);

    Room patchRoom(UUID id, Room room);
}
