package at.qe.skeleton.repositories;

import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.SensorStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SensorStationRepository extends JpaRepository<SensorStation, UUID> {

    // query by status
    List<SensorStation> findAllByStatus(DeviceStatus status);
    boolean existsByStatus(DeviceStatus status);

    // find for this room
    Optional<SensorStation> findByRoom(UUID roomId);

    // find by name
    Optional<SensorStation> findByName(String name);
    boolean existsByName(String name);
}