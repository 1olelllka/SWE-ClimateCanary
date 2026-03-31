package at.qe.skeleton.repositories;

import at.qe.skeleton.model.TemperatureLimit;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemperatureLimitRepository extends LimitValuesRepository<TemperatureLimit> {

    Optional<TemperatureLimit> findByRoomMonitoring_RoomId(UUID roomId);

    Optional<TemperatureLimit> findByRoomMonitoring_RoomIdAndVersion(UUID roomId, int version);
}
