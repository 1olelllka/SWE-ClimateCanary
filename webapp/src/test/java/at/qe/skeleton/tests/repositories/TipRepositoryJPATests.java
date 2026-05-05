package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.Tip;
import at.qe.skeleton.model.ViolatedSensor;
import at.qe.skeleton.model.ViolationType;
import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.repositories.TipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class TipRepositoryJPATests {

    @Autowired TipRepository tipRepository;

    @BeforeEach
    void setUp() {
        tipRepository.save(Tip.builder()
                .msg("Open a window.")
                .violationStatus(WarningStatus.YELLOW)
                .violationType(ViolationType.OVER)
                .violatedSensor(ViolatedSensor.TEMPERATURE)
                .build());
    }

    @Test
    @DisplayName("returns true when a tip with exact status, type and sensor exists")
    void returnsTrueOnExactMatch() {
        assertTrue(tipRepository.existsByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.TEMPERATURE));
    }

    @Test
    @DisplayName("returns false when status differs")
    void returnsFalseWhenStatusDiffers() {
        assertFalse(tipRepository.existsByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.RED, ViolationType.OVER, ViolatedSensor.TEMPERATURE));
    }

    @Test
    @DisplayName("returns false when violation type differs")
    void returnsFalseWhenTypeDiffers() {
        assertFalse(tipRepository.existsByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.TEMPERATURE));
    }

    @Test
    @DisplayName("returns false when violated sensor differs")
    void returnsFalseWhenSensorDiffers() {
        assertFalse(tipRepository.existsByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.HUMIDITY));
    }

    @Test
    @DisplayName("returns false when repository is empty")
    void returnsFalseWhenEmpty() {
        tipRepository.deleteAll();

        assertFalse(tipRepository.existsByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.TEMPERATURE));
    }

    @Test
    @DisplayName("returns optional empty when repository is empty")
    void returnsEmptyWhenRepoEmpty() {
        tipRepository.deleteAll();
        assertTrue(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(WarningStatus.YELLOW,
                ViolationType.OVER,
                ViolatedSensor.TEMPERATURE).isEmpty());
    }

    @Test
    @DisplayName("returns optional with tip when a tip with exact status, type and sensor exists")
    void returnsPresentOnExactMatch() {
        assertTrue(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.TEMPERATURE).isPresent());
    }

    @Test
    @DisplayName("returns optional empty when status differs")
    void returnsEmptyWhenStatusDiffers() {
        assertFalse(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.RED, ViolationType.OVER, ViolatedSensor.TEMPERATURE).isPresent());
    }

    @Test
    @DisplayName("returns optional empty when violation type differs")
    void returnsEmptyWhenTypeDiffers() {
        assertFalse(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.UNDER, ViolatedSensor.TEMPERATURE).isPresent());
    }

    @Test
    @DisplayName("returns optional empty when violated sensor differs")
    void returnsEmptyWhenSensorDiffers() {
        assertFalse(tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(
                WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.HUMIDITY).isPresent());
    }

}