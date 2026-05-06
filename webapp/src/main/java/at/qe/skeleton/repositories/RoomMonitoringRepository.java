package at.qe.skeleton.repositories;

import at.qe.skeleton.model.RoomMonitoring;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomMonitoringRepository extends JpaRepository<RoomMonitoring, UUID> {
}
