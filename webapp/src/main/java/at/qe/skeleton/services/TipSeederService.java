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
            "Consider opening a window."),
        new TipDef(ViolationType.OVER, ViolatedSensor.TEMPERATURE, WarningStatus.YELLOW,
            "Open windows."),
        new TipDef(ViolationType.OVER, ViolatedSensor.TEMPERATURE, WarningStatus.RED,
            "Activate air conditioning immediately."),

        // Temperature too low
        new TipDef(ViolationType.UNDER, ViolatedSensor.TEMPERATURE, WarningStatus.GREEN,
            "Consider closing windows."),
        new TipDef(ViolationType.UNDER, ViolatedSensor.TEMPERATURE, WarningStatus.YELLOW,
            "Close windows and increase heating."),
        new TipDef(ViolationType.UNDER, ViolatedSensor.TEMPERATURE, WarningStatus.RED,
            "Turn on heating immediately."),

        // Humidity too high
        new TipDef(ViolationType.OVER, ViolatedSensor.HUMIDITY, WarningStatus.GREEN,
            "Improve air circulation."),
        new TipDef(ViolationType.OVER, ViolatedSensor.HUMIDITY, WarningStatus.YELLOW,
            "Use a dehumidifier or open windows."),
        new TipDef(ViolationType.OVER, ViolatedSensor.HUMIDITY, WarningStatus.RED,
            "Ventilate the room."),

        // Humidity too low
        new TipDef(ViolationType.UNDER, ViolatedSensor.HUMIDITY, WarningStatus.GREEN,
            "Consider using a humidifier."),
        new TipDef(ViolationType.UNDER, ViolatedSensor.HUMIDITY, WarningStatus.YELLOW,
            "Use a humidifier."),
        new TipDef(ViolationType.UNDER, ViolatedSensor.HUMIDITY, WarningStatus.RED,
            "Use a humidifier immediately."),

        // CO2 too high
        new TipDef(ViolationType.OVER, ViolatedSensor.AIR, WarningStatus.GREEN,
            "Open a window briefly."),
        new TipDef(ViolationType.OVER, ViolatedSensor.AIR, WarningStatus.YELLOW,
            "Open windows to ventilate the room."),
        new TipDef(ViolationType.OVER, ViolatedSensor.AIR, WarningStatus.RED,
            "Open all windows immediately.")
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
