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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        seedDepartmentActiveWarnings(tempTip, humTip);
    }

    // dept prefix → number of active violations, equally distributed across [1,10] (Engineering = 0)
    private static final Map<String, Integer> DEPT_ACTIVE_COUNT = Map.of(
            "MAR", 1,
            "HUM", 4,
            "FIN", 7,
            "OPE", 10
    );

    private static final MeasurementType[] TYPES = {
            MeasurementType.TEMPERATURE, MeasurementType.HUMIDITY, MeasurementType.CO2
    };

    private void seedDepartmentActiveWarnings(Tip tempTip, Tip humTip) {
        List<RoomMonitoring> allRooms = roomMonitoringRepository.findAll();

        DEPT_ACTIVE_COUNT.forEach((prefix, count) -> {
            List<RoomMonitoring> deptRooms = allRooms.stream()
                    .filter(r -> r.getRoomNumber().startsWith(prefix + "-"))
                    .sorted((a, b) -> a.getRoomNumber().compareTo(b.getRoomNumber()))
                    .toList();

            if (deptRooms.isEmpty()) {
                log.warn("No rooms found for prefix '{}', skipping.", prefix);
                return;
            }

            List<Warnings> batch = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                RoomMonitoring target = deptRooms.get(i % deptRooms.size());
                MeasurementType type  = TYPES[i % TYPES.length];
                Tip tip = (type == MeasurementType.TEMPERATURE) ? tempTip : humTip;

                String msg;
                double triggered, limit;
                WarningStatus status;
                if (type == MeasurementType.TEMPERATURE) {
                    triggered = 27.0 + i * 0.3;
                    limit     = TEMP_MAX;
                    status    = WarningStatus.RED;
                    msg       = "Temperature exceeded maximum of " + limit + " °C. Current: " + String.format("%.1f", triggered) + " °C.";
                } else if (type == MeasurementType.HUMIDITY) {
                    triggered = 65.0 + i * 0.5;
                    limit     = HUM_MAX;
                    status    = i % 3 == 0 ? WarningStatus.RED : WarningStatus.YELLOW;
                    msg       = "Humidity exceeded maximum of " + limit + "%. Current: " + String.format("%.1f", triggered) + "%.";
                } else {
                    triggered = 75.0 + i * 1.0;
                    limit     = 70.0;
                    status    = WarningStatus.RED;
                    msg       = "CO₂ level exceeded maximum of " + (int) limit + ". Current: " + (int) triggered + ".";
                    tip       = null;
                }

                batch.add(Warnings.builder()
                        .roomMonitoring(target)
                        .measurementType(type)
                        .status(status)
                        .triggeredValue(triggered)
                        .activeLimitAtTime(limit)
                        .sensorWriteId(UUID.randomUUID())
                        .message(msg)
                        .createdAt(LocalDateTime.now().minusMinutes(30L + i * 10))
                        .resolvedAt(null)
                        .tip(tip)
                        .build());
            }

            warningRepository.saveAll(batch);
            log.info("Seeded {} active warnings for prefix '{}'.", count, prefix);
        });
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
