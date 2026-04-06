package at.qe.skeleton.services;

import at.qe.skeleton.model.Absence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AbsenceService {
    Page<Absence> getAllAbsencesById(UUID id, Pageable pageable);
}
