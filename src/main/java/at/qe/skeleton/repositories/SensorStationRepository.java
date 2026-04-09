package at.qe.skeleton.repositories;


import at.qe.skeleton.model.SensorStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SensorStationRepository extends JpaRepository<SensorStation, UUID> {
}