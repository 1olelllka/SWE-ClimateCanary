package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Tip;
import at.qe.skeleton.model.ViolatedSensor;
import at.qe.skeleton.model.ViolationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TipRepository extends JpaRepository<Tip, UUID> {
    List<Tip> findByViolatedSensor(ViolatedSensor sensor);

    List<Tip> findByViolationType(ViolationType type);

    List<Tip> findByViolatedSensorAndViolationType(
            ViolatedSensor sensor,
            ViolationType type
    );

}
