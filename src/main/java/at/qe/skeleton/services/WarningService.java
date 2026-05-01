package at.qe.skeleton.services;

import at.qe.skeleton.dtos.SummaryWarningDTO;
import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.dtos.WarningUpdateStatusDTO;
import at.qe.skeleton.model.Userx;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WarningService {

    List<WarningDTO> getAllActiveWarnings();

    List<WarningDTO> getAllWarningsForRoom(Userx authenticated, UUID roomId, Boolean active, LocalDate startDate, LocalDate endDate);

    // where Pi reports new violation
    WarningDTO createWarning(WarningCreateDTO dto);

    // Pi updates status change
    WarningDTO updateWarningStatus(UUID warningId, WarningUpdateStatusDTO dto);

    // Pi resolves warning
    WarningDTO resolveWarning(UUID warningId);

    // violation log for UI table
    List<WarningDTO> getViolationLog(Userx authenticated, UUID roomId);

    List<SummaryWarningDTO> getViolationLogForDepartment(UUID id, Boolean active, LocalDate startDate, LocalDate endDate);

    List<?> getDetailedViolationLogForDepartment(Userx user, UUID id, Boolean active, LocalDate startDate, LocalDate endDate);
}
