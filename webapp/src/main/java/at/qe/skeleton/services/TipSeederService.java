package at.qe.skeleton.services;

import at.qe.skeleton.model.Tip;
import at.qe.skeleton.model.ViolatedSensor;
import at.qe.skeleton.model.ViolationType;
import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.repositories.TipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipSeederService {

    private final TipRepository tipRepository;

    private record TipDef(ViolationType type, ViolatedSensor sensor, WarningStatus status, String msg) {}

    private static final List<TipDef> TIPS = List.of(
        // Temperature too high
        new TipDef(ViolationType.OVER, ViolatedSensor.TEMPERATURE, WarningStatus.GREEN,
            "Temperature is slightly above limit. Consider opening a window."),
        new TipDef(ViolationType.OVER, ViolatedSensor.TEMPERATURE, WarningStatus.YELLOW,
            "Temperature is elevated. Open windows or lower the thermostat."),
        new TipDef(ViolationType.OVER, ViolatedSensor.TEMPERATURE, WarningStatus.RED,
            "Room is too hot! Open windows and activate air conditioning immediately."),

        // Temperature too low
        new TipDef(ViolationType.UNDER, ViolatedSensor.TEMPERATURE, WarningStatus.GREEN,
            "Temperature is slightly below limit. Consider closing windows."),
        new TipDef(ViolationType.UNDER, ViolatedSensor.TEMPERATURE, WarningStatus.YELLOW,
            "Room is cool. Close windows and increase heating."),
        new TipDef(ViolationType.UNDER, ViolatedSensor.TEMPERATURE, WarningStatus.RED,
            "Room is too cold! Turn on heating and close all windows immediately."),

        // Humidity too high
        new TipDef(ViolationType.OVER, ViolatedSensor.HUMIDITY, WarningStatus.GREEN,
            "Humidity is slightly high. Improve air circulation."),
        new TipDef(ViolationType.OVER, ViolatedSensor.HUMIDITY, WarningStatus.YELLOW,
            "Humidity is elevated. Use a dehumidifier or open windows."),
        new TipDef(ViolationType.OVER, ViolatedSensor.HUMIDITY, WarningStatus.RED,
            "Humidity is too high! Activate a dehumidifier and ventilate the room."),

        // Humidity too low
        new TipDef(ViolationType.UNDER, ViolatedSensor.HUMIDITY, WarningStatus.GREEN,
            "Air is slightly dry. Consider using a humidifier."),
        new TipDef(ViolationType.UNDER, ViolatedSensor.HUMIDITY, WarningStatus.YELLOW,
            "Air is dry. Use a humidifier to improve comfort."),
        new TipDef(ViolationType.UNDER, ViolatedSensor.HUMIDITY, WarningStatus.RED,
            "Air is very dry! Use a humidifier immediately to prevent discomfort."),

        // CO2 too high
        new TipDef(ViolationType.OVER, ViolatedSensor.AIR, WarningStatus.GREEN,
            "CO₂ level is slightly elevated. Open a window briefly."),
        new TipDef(ViolationType.OVER, ViolatedSensor.AIR, WarningStatus.YELLOW,
            "CO₂ is rising. Open windows to ventilate the room."),
        new TipDef(ViolationType.OVER, ViolatedSensor.AIR, WarningStatus.RED,
            "CO₂ is critically high! Open all windows and ventilate immediately."),

        // CO2 too low (sensor fault / unusual)
        new TipDef(ViolationType.UNDER, ViolatedSensor.AIR, WarningStatus.GREEN,
            "CO₂ level is unusually low. Ensure sensors are functioning correctly."),
        new TipDef(ViolationType.UNDER, ViolatedSensor.AIR, WarningStatus.YELLOW,
            "CO₂ level is very low. Check that sensors are calibrated."),
        new TipDef(ViolationType.UNDER, ViolatedSensor.AIR, WarningStatus.RED,
            "CO₂ level is extremely low. Check sensor functionality immediately.")
    );

    @Transactional
    public void seed() {
        if (tipRepository.count() > 0) {
            log.info("Tips already seeded, skipping.");
            return;
        }

        List<Tip> tips = TIPS.stream()
                .map(d -> Tip.builder()
                        .violationType(d.type())
                        .violatedSensor(d.sensor())
                        .violationStatus(d.status())
                        .msg(d.msg())
                        .build())
                .toList();

        tipRepository.saveAll(tips);
        log.info("Seeded {} default tips.", tips.size());
    }
}
