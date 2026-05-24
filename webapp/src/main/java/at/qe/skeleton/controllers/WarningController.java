package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.ActiveViolationBuildingStats;
import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.WarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Warnings")
public class WarningController {

    private final WarningService warningsService;
    private final AuthenticatedUserService authenticatedUserService;

    @Operation(summary = "Get warnings for a room. One of Permissions Required: CAN_VIEW_OWN_OFFICE_WARNINGS, CAN_VIEW_OWN_DEPARTMENT_WARNINGS, CAN_VIEW_ALL_ROOMS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of warnings."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Room not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Unauthorized.")
    })
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
    @Operation(summary = "Create new warning. One of Permissions Required: CAN_SEND_WARNINGS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created warning with tip."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Room not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Unauthorized.")
    })
    @PostMapping("/warnings")
    @PreAuthorize("hasAuthority('CAN_SEND_WARNINGS')")
    public ResponseEntity<WarningDTO> createWarning(
            @Valid @RequestBody WarningCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(warningsService.createWarning(dto));
    }

//    // Pi updates severity (still not sure if needed)
//    @PatchMapping("/warnings/{id}/status")
//    @PreAuthorize("hasAuthority('CAN_SEND_WARNINGS')")
//    public ResponseEntity<WarningDTO> updateWarningStatus(
//            @PathVariable UUID id,
//            @Valid @RequestBody WarningUpdateStatusDTO dto) {
//        return ResponseEntity.ok(
//                warningsService.updateWarningStatus(id, dto));
//    }

    // Pi resolves warning
    @Operation(summary = "Resolve existing warnings. One of Permissions Required: CAN_SEND_WARNINGS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resolved warning."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Unauthorized.")
    })
    @PatchMapping("/warnings/{warning_id}/resolve")
    @PreAuthorize("hasAuthority('CAN_SEND_WARNINGS')")
    public ResponseEntity<WarningDTO> resolveWarning(
            @PathVariable(name = "warning_id") UUID id) {
        return ResponseEntity.ok(warningsService.resolveWarning(id));
    }

    @Operation(summary = "Get active warnings for building. One of Permissions Required: CAN_VIEW_ALL_ROOMS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Active warnings for building."),
            @ApiResponse(responseCode = "404", description = "Building not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Unauthorized.", content = @Content())
    })
    @GetMapping("/warnings/buildings/{building_id}/active")
    @PreAuthorize("hasAuthority('CAN_VIEW_ALL_ROOMS')")
    public ResponseEntity<ActiveViolationBuildingStats> getActiveViolationsForBuilding(@PathVariable(name = "building_id") UUID id) {
        return new ResponseEntity<>(warningsService.getActiveViolationsForBuilding(id), HttpStatus.OK);
    }

    @Operation(summary = "Get warnings summary for department. One of Permissions Required: CAN_VIEW_VIOLATIONS_PER_DEPARTMENT, CAN_VIEW_OWN_DEPARTMENT_WARNINGS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of warnings."),
            @ApiResponse(responseCode = "404", description = "Department not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Unauthorized/Department rights issue.")
    })
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
