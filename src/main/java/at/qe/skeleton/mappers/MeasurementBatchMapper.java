package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.MeasurementBatchDTO;
import at.qe.skeleton.dtos.ReadingDTO;
import at.qe.skeleton.model.ClimateStats;
import at.qe.skeleton.model.RoomMonitoring;
import org.springframework.stereotype.Component;

@Component
public class MeasurementBatchMapper {

    public ClimateStats mapFrom(MeasurementBatchDTO dto, RoomMonitoring roomMonitoring) {
        ClimateStats.ClimateStatsBuilder builder = ClimateStats.builder()
                .date(dto.timestamp())
                .roomMonitoring(roomMonitoring);

        for (ReadingDTO reading : dto.readings()) {
            switch (reading.type()) {
                case TEMPERATURE -> builder.tempVal((int) reading.value());
                case HUMIDITY    -> builder.humVal((int) reading.value());
                case CO2         -> builder.pollVal((int) reading.value());
            }
        }

        return builder.build();
    }
}
