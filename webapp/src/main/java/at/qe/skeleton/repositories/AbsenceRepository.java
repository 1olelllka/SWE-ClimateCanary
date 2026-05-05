package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Absence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, UUID> {
    Page<Absence> findAllByUserId(UUID userId, Pageable pageable);

    Page<Absence> findByAssignedTo(UUID id, Pageable pageable);
}
