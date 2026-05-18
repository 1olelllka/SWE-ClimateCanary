package at.qe.skeleton.services;

import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.TipRepository;
import at.qe.skeleton.repositories.WarningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarningSeederService {

    private final RoomMonitoringRepository roomMonitoringRepository;
    private final WarningRepository warningRepository;
    private final TipRepository tipRepository;

    // Must match TemperatureLimit.prePersist() default
    static final float  TEMP_MAX = 26.0f;
    static final float  HUM_MAX  = 60.0f;

    // Values used both here and in ClimateHistorySeederService to keep them consistent
    static final double TRIGGERED_TEMP = 27.5;
    static final double TRIGGERED_HUM  = 68.0;

    @Transactional
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

        Tip tempTip = tipRepository
                .findByViolationStatusAndViolationTypeAndViolatedSensor(
                        WarningStatus.RED, ViolationType.OVER, ViolatedSensor.TEMPERATURE)
                .orElseGet(() -> tipRepository.save(Tip.builder()
                        .msg("Open windows or turn on air conditioning to reduce the temperature.")
                        .violationType(ViolationType.OVER)
                        .violatedSensor(ViolatedSensor.TEMPERATURE)
                        .violationStatus(WarningStatus.RED)
                        .build()));

        Tip humTip = tipRepository
                .findByViolationStatusAndViolationTypeAndViolatedSensor(
                        WarningStatus.RED, ViolationType.OVER, ViolatedSensor.HUMIDITY)
                .orElseGet(() -> tipRepository.save(Tip.builder()
                        .msg("Use a dehumidifier or improve ventilation to reduce humidity.")
                        .violationType(ViolationType.OVER)
                        .violatedSensor(ViolatedSensor.HUMIDITY)
                        .violationStatus(WarningStatus.RED)
                        .build()));

        String tempMsg = "Temperature exceeded maximum threshold of " + TEMP_MAX + " °C. Current reading: ";
        String humMsg  = "Humidity exceeded maximum threshold of " + HUM_MAX + "%. Current reading: ";

        warningRepository.saveAll(List.of(
                build(room, MeasurementType.TEMPERATURE, WarningStatus.RED, TRIGGERED_TEMP, TEMP_MAX,
                        tempMsg + TRIGGERED_TEMP + " °C.", 90, tempTip),
                build(room, MeasurementType.TEMPERATURE, WarningStatus.RED, 28.1, TEMP_MAX,
                        tempMsg + "28.1 °C.", 60, tempTip),
                build(room, MeasurementType.TEMPERATURE, WarningStatus.RED, 29.3, TEMP_MAX,
                        tempMsg + "29.3 °C.", 30, tempTip),
                build(room, MeasurementType.HUMIDITY, WarningStatus.RED, TRIGGERED_HUM, HUM_MAX,
                        humMsg + TRIGGERED_HUM + "%.", 75, humTip),
                build(room, MeasurementType.HUMIDITY, WarningStatus.RED, 72.5, HUM_MAX,
                        humMsg + "72.5%.", 45, humTip)
        ));

        log.info("Seeded 3 temperature and 2 humidity active warnings for room {}.", room.getRoomNumber());
    }

    private Warnings build(RoomMonitoring room, MeasurementType type, WarningStatus status,
                           double triggered, double limit, String message, long minutesAgo, Tip tip) {
        return Warnings.builder()
                .roomMonitoring(room)
                .measurementType(type)
                .status(status)
                .triggeredValue(triggered)
                .activeLimitAtTime(limit)
                .sensorWriteId(UUID.randomUUID())
                .message(message)
                .createdAt(LocalDateTime.now().minusMinutes(minutesAgo))
                .resolvedAt(LocalDateTime.now().plusMinutes(5))
                .tip(tip)
                .build();
    }
}
