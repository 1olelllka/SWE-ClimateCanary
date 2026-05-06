package at.qe.skeleton.dtos;

import at.qe.skeleton.model.Trend;

public record BuildingGlobalStatusDTO(
        int totalRooms,

        int roomsGood,
        int roomsWarning,
        int roomsCritical,

        double avgAirQuility,

        double avgHumidity,

        double avgTemperature,

        Trend trend
) {
}
