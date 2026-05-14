package at.qe.skeleton.background;

import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.RoleRepository;
import at.qe.skeleton.repositories.UserxRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AbsenceDaysUpdater {

    private final UserxRepository userxRepository;

    @Async
    @Scheduled(cron = "0 0 1 1 * *")
    public void updateAllAbsencesNumbers() {
        log.info("Updating all of the absences for employees...");
        List<Userx> employees = userxRepository.findByRoleName("EMPLOYEE");
        employees.forEach(employee -> {
            employee.setNumberOfAbsences(employee.getNumberOfAbsences() + 25);
            userxRepository.save(employee);
        });
        log.info("Updating all of the absences for department managers...");
        List<Userx> deptManagers = userxRepository.findByRoleName("DEPARTMENT_MANAGER");
        deptManagers.forEach(dept -> {
            dept.setNumberOfAbsences(dept.getNumberOfAbsences() + 25);
            userxRepository.save(dept);
        });
        log.info("Successfully updated all of the absences.");
    }

}
