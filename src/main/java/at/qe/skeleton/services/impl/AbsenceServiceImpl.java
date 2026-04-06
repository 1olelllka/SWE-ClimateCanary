package at.qe.skeleton.services.impl;

import at.qe.skeleton.model.Absence;
import at.qe.skeleton.repositories.AbsenceRepository;
import at.qe.skeleton.services.AbsenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AbsenceServiceImpl implements AbsenceService {

    private AbsenceRepository absenceRepository;

    @Autowired
    public AbsenceServiceImpl(AbsenceRepository absenceRepository) {
        this.absenceRepository = absenceRepository;
    }

    @Override
    public Page<Absence> getAllAbsencesById(UUID id, Pageable pageable) {
        return absenceRepository.findAllByUserId(id, pageable);
    }
}
