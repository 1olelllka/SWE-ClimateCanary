package at.qe.skeleton.initializer;

import at.qe.skeleton.model.ClimateStats;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.repositories.ClimateStatsRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Profile("dev") // only runs in dev, not in production
public class ClimateDataInitializer implements ApplicationRunner {

    private final ClimateStatsRepository climateStatsRepository;
    private final RoomMonitoringRepository roomMonitoringRepository;
    private final Random random = new Random();

    public ClimateDataInitializer(ClimateStatsRepository climateStatsRepository,
                                  RoomMonitoringRepository roomMonitoringRepository) {
        this.climateStatsRepository = climateStatsRepository;
        this.roomMonitoringRepository = roomMonitoringRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // only seed if empty, so it doesn't duplicate on every restart
        if (climateStatsRepository.count() > 0) return;

        List<RoomMonitoring> rooms = roomMonitoringRepository.findAll();

        for (RoomMonitoring room : rooms) {
            generateDataForRoom(room);
        }
    }

    private void generateDataForRoom(RoomMonitoring room) {
        // generate one data point every 5 minutes for the last 30 days
        LocalDateTime start = LocalDateTime.now().minusDays(30);

        List<ClimateStats> dataPoints = new ArrayList<>();

        for (LocalDateTime time = start; time.isBefore(LocalDateTime.now()); time = time.plusMinutes(5)) {
            dataPoints.add(ClimateStats.builder()
                    .roomMonitoring(room)
                    .date(time)
                    .tempVal(generateTemperature(time))
                    .humVal(generateHumidity(time))
                    .pollVal(generateAirQuality(time))
                    .build());
        }

        climateStatsRepository.saveAll(dataPoints);
    }

    private double generateTemperature(LocalDateTime time) {
        int hour = time.getHour();
        boolean isWorkHour = hour >= 8 && hour <= 18;
        boolean isWeekend = time.getDayOfWeek() == DayOfWeek.SATURDAY
                || time.getDayOfWeek() == DayOfWeek.SUNDAY;

        double base = (isWorkHour && !isWeekend) ? 22.0 : 19.0;
        double noise = (random.nextDouble() - 0.5) * 1.5; // ±0.75°C noise
        return Math.round((base + noise) * 10.0) / 10.0;
    }

    private double generateHumidity(LocalDateTime time) {
        int hour = time.getHour();
        boolean isWorkHour = hour >= 8 && hour <= 18;

        double base = isWorkHour ? 52.0 : 42.0; // higher when people are present
        double noise = (random.nextDouble() - 0.5) * 6.0;
        return Math.round((base + noise) * 10.0) / 10.0;
    }

    private double generateAirQuality(LocalDateTime time) {
        int hour = time.getHour();
        // CO2 ppm: rises during work hours, spikes before lunch
        double base = 400;
        if (hour >= 8 && hour <= 18) base = 750;
        if (hour >= 11 && hour <= 13) base = 1050; // lunch spike

        double noise = (random.nextDouble() - 0.5) * 100;
        return Math.round((base + noise) * 10.0) / 10.0;
    }
}
