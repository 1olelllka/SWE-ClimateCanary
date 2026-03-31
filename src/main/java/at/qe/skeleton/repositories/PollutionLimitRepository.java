package at.qe.skeleton.repositories;

import at.qe.skeleton.model.PollutionLimit;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollutionLimitRepository extends LimitValuesRepository<PollutionLimit> {

    Optional<PollutionLimit> findByRoomMonitoring_RoomId(UUID roomId);

    Optional<PollutionLimit> findByRoomMonitoring_RoomIdAndVersion(UUID roomId, int version);
}
