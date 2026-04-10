package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.model.Absence;
import at.qe.skeleton.model.AbsenceStatus;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.AbsenceRepository;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.services.AbsenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AbsenceServiceImpl implements AbsenceService {

    private AbsenceRepository absenceRepository;
    private UserxRepository userxRepository;

    @Autowired
    public AbsenceServiceImpl(AbsenceRepository absenceRepository,
                              UserxRepository userxRepository) {
        this.absenceRepository = absenceRepository;
        this.userxRepository = userxRepository;
    }

    @Override
    public Page<Absence> getAllAbsencesById(UUID id, Pageable pageable) {
        return absenceRepository.findAllByUserId(id, pageable);
    }

    @Override
    @Transactional
    public Absence createNewAbsenceForUser(Absence absence) {
        if (absence.getAssignedTo().equals(absence.getUser().getId())) {
            throw new ValidationException("Assigned person must not be the same as you.");
        }
        Optional<Userx> manager = userxRepository.findById(absence.getAssignedTo());
        if (manager.isEmpty()) {
            throw new NotFoundException("Manager with id " + absence.getAssignedTo() + " was not found.");
        }
        Set<String> authorities = manager.get().getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        if (!authorities.contains("CAN_MANAGE_ABSENCES")) {
            throw new ForbiddenException("Assigned person does not have manager rights.");
        }
        UUID id = absence.getUser().getId();
        Userx user = userxRepository.findById(id).orElseThrow(() -> new NotFoundException("User with id " + id + " was not found"));
        if (!user.getMyRoom().getDepartment().getId().equals(manager.get().getMyRoom().getDepartment().getId())) {
            throw new ForbiddenException("You cannot apply for absence to this manager.");
        }
        return absenceRepository.save(absence);
    }

    @Override
    public Absence getAbsenceById(UUID id, Userx manager) {
        Absence absence = absenceRepository.findById(id).orElseThrow(() -> new NotFoundException("Absence with id " + id + " was not found."));
        if (absence.getAssignedTo().equals(manager.getId())) {
            return absence;
        }
        throw new ForbiddenException("This absence was not assigned to you.");
    }

    @Override
    public void deleteAbsenceById(UUID id, Userx user) {
        Absence absence = absenceRepository.findById(id).orElseThrow(() -> new NotFoundException("Absence with id " + id + " was not found."));
        if (user.getId().equals(absence.getUser().getId())) {
            absenceRepository.deleteById(id);
            return;
        }
        throw new ForbiddenException("You are not allowed to delete this absence.");
    }

    @Override
    @Transactional
    public Absence updateAbsenceStatus(UUID id, AbsenceStatus status) {
        Absence absence = absenceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Absence with id " + id + " was not found."));
        Optional<Userx> manager = userxRepository.findById(absence.getAssignedTo());
        if (manager.isEmpty()) {
            throw new NotFoundException("Manager with id " + absence.getAssignedTo() + " was not found.");
        }
        Set<String> authorities = manager.get().getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        if (!authorities.contains("CAN_MANAGE_ABSENCES")) {
            throw new ForbiddenException("Assigned person does not have manager rights.");
        }
        UUID userId = absence.getUser().getId();
        Userx user = userxRepository.findById(userId).orElseThrow(() -> new NotFoundException("User with id " + userId + " was not found"));
        if (!user.getMyRoom().getDepartment().getId().equals(manager.get().getMyRoom().getDepartment().getId())) {
            throw new ForbiddenException("You cannot update absence status for this employee.");
        }
        absence.setStatus(status);
        return absenceRepository.save(absence);
    }

    @Override
    public Page<Absence> getAllAbsencesByDepartment(Userx user, Pageable pageable) {
        return absenceRepository.findByAssignedTo(user.getId(), pageable);
    }
}
