package at.qe.skeleton.repositories;

import at.qe.skeleton.model.HumidityLimit;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HumidityLimitRepository extends LimitValuesRepository<HumidityLimit> {

    Optional<HumidityLimit> findByRoomMonitoring_RoomId(UUID roomId);

    Optional<HumidityLimit> findByRoomMonitoring_RoomIdAndVersion(UUID roomId, int version);
}
