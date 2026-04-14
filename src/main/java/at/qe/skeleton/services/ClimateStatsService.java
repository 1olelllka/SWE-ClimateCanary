package at.qe.skeleton.services;

import at.qe.skeleton.dtos.AggregatedDataPointDTO;
import at.qe.skeleton.dtos.ClimateDataPointDTO;
import at.qe.skeleton.dtos.MeasurementBatchDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ClimateStatsService {

    ClimateDataPointDTO getCurrentClimate(UUID roomId);

    void saveMeasurementBatch(MeasurementBatchDTO batch);

    List<ClimateDataPointDTO> getOvertime(UUID roomId,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          LocalTime startTime,
                                          LocalTime endTime);

    // full granularity
    List<AggregatedDataPointDTO> getClimateHistoryFull(UUID roomId,
                                                       String timeframe,
                                                       String granularity);

    // Reduced granularity (hourly aggregated using background job)
    List<AggregatedDataPointDTO> getClimateHistoryReduced(UUID roomId,
                                                          String timeframe);
}
