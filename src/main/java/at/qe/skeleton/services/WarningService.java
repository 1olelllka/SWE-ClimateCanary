package at.qe.skeleton.services;

import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.dtos.WarningUpdateStatusDTO;

import java.util.List;
import java.util.UUID;

public interface WarningService {

    List<WarningDTO> getAllActiveWarnings();

    List<WarningDTO> getActiveWarningsForRoom(UUID roomId);

    // where Pi reports new violation
    WarningDTO createWarning(WarningCreateDTO dto);

    // Pi updates status change
    WarningDTO updateWarningStatus(UUID warningId, WarningUpdateStatusDTO dto);

    // Pi resolves warning
    WarningDTO resolveWarning(UUID warningId);

    // violation log for UI table
    List<WarningDTO> getViolationLog(UUID roomId);
}
