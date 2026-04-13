package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.ClimateDataPointDTO;
import at.qe.skeleton.services.ClimateStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/rooms")
public class ClimateStatsController {

    private final ClimateStatsService climateStatsService;

    public ClimateStatsController(ClimateStatsService climateStatsService) {
        this.climateStatsService = climateStatsService;
    }

    @GetMapping("/{id}/current-climate")
    public ResponseEntity<ClimateDataPointDTO> getCurrentClimate(@PathVariable UUID id) {
        return ResponseEntity.ok(climateStatsService.getCurrentClimate(id));
    }
}