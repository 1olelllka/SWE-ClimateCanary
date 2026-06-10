package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.AggregatedDataPointDTO;
import at.qe.skeleton.dtos.ClimateDataPointDTO;
import at.qe.skeleton.dtos.MeasurementBatchDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.services.ClimateStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Climate Endpoints")
public class ClimateStatsController {

    private final ClimateStatsService climateStatsService;

    @Operation(summary = "Post measurement. One of Permissions Required: CAN_SEND_MEASUREMENTS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created measurement."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Room not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
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

    @Operation(summary = "Get current climate. One of Permissions Required: CAN_VIEW_ALL_ROOMS, CAN_VIEW_OWN_SHARED_CLIMATE, CAN_VIEW_OWN_OFFICE_CLIMATE, CAN_VIEW_OWN_DEPARTMENT_MEASURES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current climate of a room."),
            @ApiResponse(responseCode = "403", description = "Right/Rooms issues.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Room not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @GetMapping("/rooms/{id}/current-climate")
    public ResponseEntity<ClimateDataPointDTO> getCurrentClimate(
            @PathVariable UUID id) {
        return ResponseEntity.ok(climateStatsService.getCurrentClimate(id));
    }

    @Operation(summary = "Get climate overtime. One of Permissions Required: CAN_VIEW_ALL_ROOMS, CAN_VIEW_OWN_SHARED_CLIMATE, CAN_VIEW_OWN_OFFICE_CLIMATE, CAN_VIEW_OWN_DEPARTMENT_MEASURES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Overtime of a room."),
            @ApiResponse(responseCode = "403", description = "Right/Rooms issues.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "Validation issues.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Room not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
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

    @Operation(summary = "Get climate history (full & reduced). One of Permissions Required: CAN_VIEW_ALL_ROOMS, CAN_VIEW_OWN_SHARED_CLIMATE, CAN_VIEW_OWN_OFFICE_CLIMATE, CAN_VIEW_OWN_DEPARTMENT_MEASURES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Climate History."),
            @ApiResponse(responseCode = "403", description = "Right/Rooms issues.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "Validation issues.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Room not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
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

    @Operation(summary = "Get Department's last aggregation. One of Permissions Required: CAN_VIEW_COMPANY_AGGR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Last aggregated data for department."),
            @ApiResponse(responseCode = "404", description = "Room not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @GetMapping("/departments/{id}/last-aggregation")
    public ResponseEntity<AggregatedDataPointDTO> getAggregatedDataForDepartment(@PathVariable(name = "id") UUID departmentId) {
        return new ResponseEntity<>(climateStatsService.getDepartmentAggregatedData(departmentId), HttpStatus.OK);
    }

    @Operation(summary = "Get Department's aggregations. One of Permissions Required: CAN_VIEW_COMPANY_AGGR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All of aggregated data for department."),
            @ApiResponse(responseCode = "400", description = "Timestamps issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Room not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
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