package at.qe.skeleton.services;

import at.qe.skeleton.model.MeasurementType;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.model.Warnings;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.WarningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarningSeederService {

    private final RoomMonitoringRepository roomMonitoringRepository;
    private final WarningRepository warningRepository;

    // Must match TemperatureLimit.prePersist() default
    static final float TEMP_MAX = 26.0f;

    // Value used both here and in ClimateHistorySeederService to keep them consistent
    static final double TRIGGERED_TEMP = 27.5;

    public void seed() {
        if (warningRepository.count() > 0) {
            log.info("Warnings already seeded, skipping.");
            return;
        }

        RoomMonitoring room = roomMonitoringRepository.findAll().stream()
                .filter(r -> r.getRoomNumber().endsWith("101"))
                .findFirst()
                .orElse(null);

        if (room == null) {
            log.warn("Room *-101 not found, skipping warning seeding.");
            return;
        }

        Warnings warning = Warnings.builder()
                .roomMonitoring(room)
                .measurementType(MeasurementType.TEMPERATURE)
                .status(WarningStatus.RED)
                .triggeredValue(TRIGGERED_TEMP)
                .activeLimitAtTime(TEMP_MAX)
                .message("Temperature exceeded maximum threshold of " + TEMP_MAX
                        + " °C. Current reading: " + TRIGGERED_TEMP
                        + " °C. ")
                .createdAt(LocalDateTime.now().minusMinutes(45))
                .resolvedAt(null)
                .build();

        warningRepository.save(warning);
        log.info("Seeded active RED temperature warning for room {}.", room.getRoomNumber());
    }
}
