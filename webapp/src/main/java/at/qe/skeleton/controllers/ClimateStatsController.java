package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.AggregatedDataPointDTO;
import at.qe.skeleton.dtos.ClimateDataPointDTO;
import at.qe.skeleton.dtos.MeasurementBatchDTO;
import at.qe.skeleton.services.ClimateStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClimateStatsController {

    private final ClimateStatsService climateStatsService;

    @PostMapping("/measurements")
    public ResponseEntity<Void> postMeasurements(
            @Valid @RequestBody MeasurementBatchDTO batch) {
        climateStatsService.saveMeasurementBatch(batch);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/rooms/{id}/current-climate")
    public ResponseEntity<ClimateDataPointDTO> getCurrentClimate(
            @PathVariable UUID id) {
        return ResponseEntity.ok(climateStatsService.getCurrentClimate(id));
    }

    @GetMapping("/rooms/{id}/overtime")
    public ResponseEntity<List<ClimateDataPointDTO>> getOvertime(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        return ResponseEntity.ok(
                climateStatsService.getOvertime(id, startDate, endDate, startTime, endTime));
    }

    @GetMapping("/rooms/{id}/climate-history")
    public ResponseEntity<List<AggregatedDataPointDTO>> getClimateHistory(
            @PathVariable UUID id,
//            @RequestParam String timeframe,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String granularity,
            @RequestParam(defaultValue = "false") boolean isGeneralArea) {

        // Department manager viewing an office forced to see day averages (privacy)
        if (hasRole("DEPARTMENT_MANAGER") && !isGeneralArea) {
                return ResponseEntity.ok(
                    climateStatsService.getClimateHistoryReduced(id, startDate, endDate, granularity));
        }

        // Everyone else can view full granularity with smart timeframe defaults
        return ResponseEntity.ok(
                climateStatsService.getClimateHistoryFull(id, startDate, endDate, granularity));
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}