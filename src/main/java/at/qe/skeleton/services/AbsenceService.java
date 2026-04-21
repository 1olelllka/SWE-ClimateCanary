package at.qe.skeleton.services;

import at.qe.skeleton.model.Absence;
import at.qe.skeleton.model.AbsenceStatus;
import at.qe.skeleton.model.Userx;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AbsenceService {
    Page<Absence> getAllAbsencesById(UUID id, Pageable pageable);

    Absence createNewAbsenceForUser(Absence absence);

    Absence getAbsenceById(UUID id, Userx manager);

    void deleteAbsenceById(UUID id, Userx user);

    Absence updateAbsenceStatus(UUID id, @NotNull(message = "Status must not be null.") AbsenceStatus status);

    Page<Absence> getAllAbsencesByDepartment(Userx user, Pageable pageable);

    void clockIn(Userx user);

    void clockOut(Userx user);
}
