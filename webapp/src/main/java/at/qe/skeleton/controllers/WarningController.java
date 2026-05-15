package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.dtos.WarningUpdateStatusDTO;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.WarningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class WarningController {

    private final WarningService warningsService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/warnings/rooms/{room_id}")
    @PreAuthorize("hasAuthority('CAN_VIEW_OWN_OFFICE_WARNINGS') or hasAuthority('CAN_VIEW_OWN_DEPARTMENT_WARNINGS') or hasAuthority('CAN_VIEW_ALL_ROOMS')")
    public ResponseEntity<List<WarningDTO>> getWarningsForRoom(
            @PathVariable(name = "room_id") UUID roomId,
            @RequestParam(name = "activeOnly") boolean active,
            @RequestParam(name = "startDate") LocalDate startDate,
            @RequestParam(name = "endDate") LocalDate endDate) {
        Userx authenticated = authenticatedUserService.getAuthenticatedUser();
        return ResponseEntity.ok(
                warningsService.getAllWarningsForRoom(authenticated, roomId, active, startDate, endDate));
    }

    // Pi reports a new warning
    @PostMapping("/warnings")
    @PreAuthorize("hasAuthority('CAN_SEND_WARNINGS')")
    public ResponseEntity<WarningDTO> createWarning(
            @Valid @RequestBody WarningCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(warningsService.createWarning(dto));
    }

    // Pi updates severity (still not sure if needed)
    @PatchMapping("/warnings/{id}/status")
    @PreAuthorize("hasAuthority('CAN_SEND_WARNINGS')")
    public ResponseEntity<WarningDTO> updateWarningStatus(
            @PathVariable UUID id,
            @Valid @RequestBody WarningUpdateStatusDTO dto) {
        return ResponseEntity.ok(
                warningsService.updateWarningStatus(id, dto));
    }

    // Pi resolves warning
    @PatchMapping("/warnings/{warning_id}/resolve")
    @PreAuthorize("hasAuthority('CAN_SEND_WARNINGS')")
    public ResponseEntity<WarningDTO> resolveWarning(
            @PathVariable(name = "warning_id") UUID id) {
        return ResponseEntity.ok(warningsService.resolveWarning(id));
    }

    @GetMapping("/warnings/departments/{department_id}/summary")
    @PreAuthorize("hasAuthority('CAN_VIEW_VIOLATIONS_PER_DEPARTMENT') or hasAuthority('CAN_VIEW_OWN_DEPARTMENT_WARNINGS')")
    public ResponseEntity<List<?>> getWarningsSummaryForDepartment(
            @PathVariable(name = "department_id") UUID id,
            @RequestParam(name = "onlyActive") boolean active,
            @RequestParam(name = "startDate") LocalDate startDate,
            @RequestParam(name = "endDate") LocalDate endDate) {
        Userx user = authenticatedUserService.getAuthenticatedUser();
        List<?> dtos;
        if (user.getAuthorities().stream().anyMatch(s -> s.getAuthority().equals("CAN_VIEW_VIOLATIONS_PER_DEPARTMENT"))) {
            dtos = warningsService.getViolationLogForDepartment(id, active, startDate, endDate);
        } else {
            dtos = warningsService.getDetailedViolationLogForDepartment(user, id, active, startDate, endDate);
        }
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }
}
