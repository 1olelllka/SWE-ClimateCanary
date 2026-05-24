package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.BuildingTrendDTO;
import at.qe.skeleton.model.BuildingTrend;
import at.qe.skeleton.services.BuildingTrendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/building-trend")
@Tag(name = "Building Trend", description = "Calculating building trend")
public class BuildingTrendController {

    private final BuildingTrendService trendService;

    @Operation(summary = "Trend per department. One of Permissions Required: CAN_VIEW_COMPANY_AGGR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of trends of department"),
            @ApiResponse(responseCode = "400", description = "Timestamp issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @GetMapping("/departments/{department_id}")
    public ResponseEntity<List<BuildingTrendDTO>> getTrendsForSpecificDepartment(@PathVariable(name = "department_id")UUID id,
                                                                                 @RequestParam(name = "startDate")LocalDate startDate,
                                                                                 @RequestParam(name = "endDate") LocalDate endDate) {
        List<BuildingTrend> trends = trendService.getTrendsForDepartment(id, startDate, endDate);
        return new ResponseEntity<>(trends.stream().map(trend ->
            new BuildingTrendDTO(trend.getId(),
                    trend.getDepartmentId(),
                    trend.getDepartmentName(),
                    trend.getTrend(),
                    trend.getValue(),
                    trend.getDate())
        ).toList(), HttpStatus.OK);
    }

}
