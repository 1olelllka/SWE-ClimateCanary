package at.qe.skeleton.repositories;

import at.qe.skeleton.model.HumidityLimit;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HumidityLimitRepository extends org.springframework.data.jpa.repository.JpaRepository<HumidityLimit, UUID> {

}
