package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.BuildingTrendDTO;
import at.qe.skeleton.model.BuildingTrend;
import at.qe.skeleton.services.BuildingTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/building-trend")
public class BuildingTrendController {

    private final BuildingTrendService trendService;

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
