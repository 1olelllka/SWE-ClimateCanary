package at.qe.skeleton.repositories;

import at.qe.skeleton.model.RoomMonitoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomMonitoringRepository extends JpaRepository<RoomMonitoring, UUID> {

    // existence check
    boolean existsByRoomId(UUID roomId);
}
