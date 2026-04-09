package at.qe.skeleton.repositories;

import at.qe.skeleton.model.PollutionLimit;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PollutionLimitRepository extends org.springframework.data.jpa.repository.JpaRepository<PollutionLimit, UUID> {
}
