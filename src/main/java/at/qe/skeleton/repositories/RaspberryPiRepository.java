package at.qe.skeleton.repositories;

import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RaspberryPiRepository extends JpaRepository<RaspberryPi, UUID> {

    // query by status
    List<RaspberryPi> findAllByStatus(DeviceStatus status);
    boolean existsByStatus(DeviceStatus status);

    // find for this room
    Optional<RaspberryPi> findByRoom(UUID roomId);

    // ip lookup
    Optional<RaspberryPi> findByIp(String ip);
    boolean existsByIp(String ip);

    // find with violations number
    List<RaspberryPi> findAllByViolationCounterGreaterThan(int threshold);
}
