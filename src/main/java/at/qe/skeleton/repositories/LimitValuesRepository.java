package at.qe.skeleton.repositories;

import at.qe.skeleton.model.LimitValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimitValuesRepository<T extends LimitValues> extends JpaRepository<T, UUID> {

    Optional<T> findTopByOrderByVersionDesc();

    List<T> findAllByOrderByVersionDesc();

    Optional<T> findByVersion(int version);

    boolean existsByVersion(int version);
}
