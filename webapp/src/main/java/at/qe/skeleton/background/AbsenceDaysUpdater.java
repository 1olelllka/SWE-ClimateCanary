package at.qe.skeleton.background;

import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.UserxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled job that increments the annual absence allowance for all employees
 * and department managers. Runs once a year on January 1st at 01:00.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AbsenceDaysUpdater {

    private final UserxRepository userxRepository;

    /**
     * Adds 25 absence days to every user with the role {@code EMPLOYEE} or
     * {@code DEPARTMENT_MANAGER}. Scheduled to run once a year on January 1st
     * ({@code 0 0 1 1 * *}) and executed asynchronously.
     */
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