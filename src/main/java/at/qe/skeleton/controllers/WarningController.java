package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.dtos.WarningUpdateStatusDTO;
import at.qe.skeleton.services.WarningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WarningController {

    private final WarningService warningsService;


    @GetMapping("/warnings")
    public ResponseEntity<List<WarningDTO>> getActiveWarnings(
            @RequestParam(required = false) UUID roomId) {
        if (roomId != null) {
            return ResponseEntity.ok(
                    warningsService.getActiveWarningsForRoom(roomId));
        }
        return ResponseEntity.ok(warningsService.getAllActiveWarnings());
    }

    // Pi reports a new warning
    @PostMapping("/warnings")
    public ResponseEntity<WarningDTO> createWarning(
            @Valid @RequestBody WarningCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(warningsService.createWarning(dto));
    }

    // Pi updates severity (still not sure if needed)
    @PatchMapping("/warnings/{id}/status")
    public ResponseEntity<WarningDTO> updateWarningStatus(
            @PathVariable UUID id,
            @Valid @RequestBody WarningUpdateStatusDTO dto) {
        return ResponseEntity.ok(
                warningsService.updateWarningStatus(id, dto));
    }

    // Pi resolves warning
    @PatchMapping("/warnings/{id}/resolve")
    public ResponseEntity<WarningDTO> resolveWarning(
            @PathVariable UUID id) {
        return ResponseEntity.ok(warningsService.resolveWarning(id));
    }

    // full violation log for UI table
    @GetMapping("/api/rooms/{id}/violations")
    public ResponseEntity<List<WarningDTO>> getViolationLog(
            @PathVariable UUID id) {
        return ResponseEntity.ok(warningsService.getViolationLog(id));
    }
}
