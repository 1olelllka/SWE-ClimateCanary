package at.qe.skeleton.repositories;

import at.qe.skeleton.model.TemperatureLimit;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TemperatureLimitRepository extends org.springframework.data.jpa.repository.JpaRepository<TemperatureLimit, UUID> {
}
