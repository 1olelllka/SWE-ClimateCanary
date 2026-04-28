package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    boolean existsByRoomNumber(String roomNumber);
    Optional<Room> findByRoomNumber(String roomNumber);
}