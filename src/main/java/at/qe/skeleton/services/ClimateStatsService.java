package at.qe.skeleton.services;

import at.qe.skeleton.dtos.ClimateDataPointDTO;

import java.util.UUID;

public interface ClimateStatsService {
    ClimateDataPointDTO getCurrentClimate(UUID roomId);
}
