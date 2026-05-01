package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Tip;
import at.qe.skeleton.model.ViolatedSensor;
import at.qe.skeleton.model.ViolationType;
import at.qe.skeleton.model.WarningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;
import java.util.UUID;

@Repository
public interface TipRepository extends JpaRepository<Tip, UUID> {

    boolean existsByViolationStatusAndViolationTypeAndViolatedSensor(WarningStatus status,
                                                                     ViolationType type,
                                                                     ViolatedSensor sensor);

    Optional<Tip> findByViolationStatusAndViolationTypeAndViolatedSensor(WarningStatus status,
                                                                         ViolationType type,
                                                                         ViolatedSensor sensor);
}
