package at.qe.skeleton.tests.services;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.TipRepository;
import at.qe.skeleton.repositories.WarningRepository;
import at.qe.skeleton.services.impl.TipServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TipService")
public class TipServiceUnitTests {

    @Mock TipRepository     tipRepository;
    @Mock WarningRepository warningRepository;

    @InjectMocks TipServiceImpl service;

    private UUID tipId;
    private Tip  sampleTip;

    @BeforeEach
    void setUp() {
        tipId = UUID.randomUUID();
        sampleTip = Tip.builder()
                .id(tipId)
                .msg("Open a window.")
                .violationStatus(WarningStatus.YELLOW)
                .violationType(ViolationType.OVER)
                .violatedSensor(ViolatedSensor.TEMPERATURE)
                .build();
    }

    // ── createTip ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTip")
    class CreateTip {

        @Test
        @DisplayName("saves and returns tip when no duplicate exists")
        void savesWhenNoDuplicate() {
            when(tipRepository.existsByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.TEMPERATURE))
                    .thenReturn(false);
            when(tipRepository.save(sampleTip)).thenReturn(sampleTip);

            Tip result = service.createTip(sampleTip);

            assertThat(result).isEqualTo(sampleTip);
            verify(tipRepository).save(sampleTip);
        }

        @Test
        @DisplayName("throws ConflictException and never saves when duplicate combination exists")
        void throwsConflictOnDuplicate() {
            when(tipRepository.existsByViolationStatusAndViolationTypeAndViolatedSensor(
                    WarningStatus.YELLOW, ViolationType.OVER, ViolatedSensor.TEMPERATURE))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createTip(sampleTip))
                    .isInstanceOf(ConflictException.class);

            verify(tipRepository, never()).save(any());
        }
    }

    // ── getAllTips ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllTips")
    class GetAllTips {

        @Test
        @DisplayName("delegates to repository and returns all tips")
        void returnsAll() {
            when(tipRepository.findAll()).thenReturn(List.of(sampleTip));

            List<Tip> result = service.getAllTips();

            assertThat(result).containsExactly(sampleTip);
            verify(tipRepository).findAll();
        }

        @Test
        @DisplayName("returns empty list when no tips exist")
        void returnsEmpty() {
            when(tipRepository.findAll()).thenReturn(List.of());

            assertThat(service.getAllTips()).isEmpty();
        }
    }

    // ── updateExistingTip ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateExistingTip")
    class UpdateExistingTip {

        @Test
        @DisplayName("updates message and returns saved tip")
        void updatesMessage() {
            when(tipRepository.findById(tipId)).thenReturn(Optional.of(sampleTip));
            when(tipRepository.save(sampleTip)).thenAnswer(i -> i.getArgument(0));

            Tip result = service.updateExistingTip(tipId, "Keep the AC on.");

            assertThat(result.getMsg()).isEqualTo("Keep the AC on.");
            verify(tipRepository).save(sampleTip);
        }

        @Test
        @DisplayName("throws NotFoundException when tip does not exist")
        void throwsWhenNotFound() {
            when(tipRepository.findById(tipId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateExistingTip(tipId, "anything"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(tipId.toString());

            verify(tipRepository, never()).save(any());
        }
    }

    // ── deleteTip ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteTip")
    class DeleteTip {

        @Test
        @DisplayName("unlinks all associated warnings and deletes tip")
        void unlinksWarningsAndDeletes() {
            Warnings w1 = new Warnings();
            Warnings w2 = new Warnings();
            sampleTip.setWarnings(new ArrayList<>(List.of(w1, w2)));

            when(tipRepository.findById(tipId)).thenReturn(Optional.of(sampleTip));

            service.deleteTip(tipId);

            assertThat(w1.getTip()).isNull();
            assertThat(w2.getTip()).isNull();
            verify(warningRepository, times(2)).save(any(Warnings.class));
            verify(tipRepository).deleteById(tipId);
        }

        @Test
        @DisplayName("deletes tip with no associated warnings without touching warningRepository")
        void deletesWithNoWarnings() {
            sampleTip.setWarnings(new ArrayList<>());
            when(tipRepository.findById(tipId)).thenReturn(Optional.of(sampleTip));

            service.deleteTip(tipId);

            verifyNoInteractions(warningRepository);
            verify(tipRepository).deleteById(tipId);
        }

        @Test
        @DisplayName("does nothing when tip does not exist (NOTE: inconsistent with updateExistingTip which throws)")
        void doesNothingWhenNotFound() {
            when(tipRepository.findById(tipId)).thenReturn(Optional.empty());

            service.deleteTip(tipId);

            verify(tipRepository, never()).deleteById(any());
            verifyNoInteractions(warningRepository);
        }
    }
}