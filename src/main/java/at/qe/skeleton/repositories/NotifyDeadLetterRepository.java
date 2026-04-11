package at.qe.skeleton.repositories;

import at.qe.skeleton.model.NotifyDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotifyDeadLetterRepository extends JpaRepository<NotifyDeadLetter, UUID> {
//    List<NotifyDeadLetter> findByRaspberryPi(UUID id);
}
