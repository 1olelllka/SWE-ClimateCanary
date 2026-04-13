package at.qe.skeleton.services.impl;

import at.qe.skeleton.dtos.ClimateDataPointDTO;
import at.qe.skeleton.model.ClimateStats;
import at.qe.skeleton.repositories.ClimateStatsRepository;
import at.qe.skeleton.services.ClimateStatsService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ClimateStatsServiceImpl implements ClimateStatsService {

    private final ClimateStatsRepository climateStatsRepository;

    public ClimateStatsServiceImpl(ClimateStatsRepository climateStatsRepository) {
        this.climateStatsRepository = climateStatsRepository;
    }

    @Override
    public ClimateDataPointDTO getCurrentClimate(UUID roomId) {
        ClimateStats latest = climateStatsRepository
                .findByRoom(roomId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No climate data found for room " + roomId));

        return new ClimateDataPointDTO(
                latest.getDate(),
                latest.getTempVal(),
                latest.getHumVal(),
                latest.getPollVal()
        );
    }
}
