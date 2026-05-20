package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.AggregatedDataPointDTO;
import at.qe.skeleton.dtos.ClimateDataPointDTO;
import at.qe.skeleton.dtos.MeasurementBatchDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.services.ClimateStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClimateStatsController {

    private final ClimateStatsService climateStatsService;

    @PostMapping("/measurements")
    public ResponseEntity<Void> postMeasurements(
            @Valid @RequestBody MeasurementBatchDTO batch,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
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
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String granularity,
            @RequestParam(defaultValue = "false") boolean isGeneralArea) {

        // Department manager viewing an office forced to see day averages (privacy)
        if (!hasRole("CAN_VIEW_ALL_ROOMS") && hasRole("CAN_VIEW_OWN_DEPARTMENT_MEASURES") && !isGeneralArea) {
                return ResponseEntity.ok(
                    climateStatsService.getClimateHistoryReduced(id, startDate, endDate, granularity));
        }

        // Everyone else can view full granularity with smart timeframe defaults
        return ResponseEntity.ok(
                climateStatsService.getClimateHistoryFull(id, startDate, endDate, granularity));
    }

    @GetMapping("/departments/{id}/last-aggregation")
    public ResponseEntity<AggregatedDataPointDTO> getAggregatedDataForDepartment(@PathVariable(name = "id") UUID departmentId) {
        return new ResponseEntity<>(climateStatsService.getDepartmentAggregatedData(departmentId), HttpStatus.OK);
    }

    @GetMapping("/departments/{id}/climate-aggregation")
    public ResponseEntity<List<AggregatedDataPointDTO>> getAggregatedDataInPeriodOfTime(@PathVariable(name = "id") UUID departmentId,
                                                                                        @RequestParam(name = "startDate") LocalDate startDate,
                                                                                        @RequestParam(name = "endDate") LocalDate endDate) {
        return new ResponseEntity<>(climateStatsService.getDepartmentAggregatedDataInTimePeriod(departmentId, startDate, endDate), HttpStatus.OK);
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}