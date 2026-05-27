package at.qe.skeleton.tests.services;

import at.qe.skeleton.dtos.ClimateDataPointDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.model.MeasurementType;
import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.services.LiveDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LiveDataService")
class LiveDataServiceUnitTests {

    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks LiveDataService liveDataService;

    private final UUID roomId = UUID.randomUUID();
    private final UUID deptId = UUID.randomUUID();

    // ── helpers ───────────────────────────────────────────────────────────────

    private WarningDTO sampleWarning() {
        return new WarningDTO(
                UUID.randomUUID(),
                roomId,
                UUID.randomUUID(),
                MeasurementType.TEMPERATURE,
                WarningStatus.YELLOW,
                "Too hot",
                28.5,
                25.0,
                LocalDateTime.of(2024, 6, 15, 12, 0),
                null,
                null,
                false
        );
    }

    private ClimateDataPointDTO sampleClimateData() {
        return new ClimateDataPointDTO(
                OffsetDateTime.of(LocalDateTime.of(2024, 6, 15, 12, 0), ZoneOffset.UTC),
                22.5,
                55.0,
                800.0
        );
    }

    private record SentMessage(String destination, String payload) {}

    private SentMessage capture() {
        ArgumentCaptor<String> destCaptor    = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(destCaptor.capture(), payloadCaptor.capture());
        return new SentMessage(destCaptor.getValue(), payloadCaptor.getValue().toString());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // pushActiveWarning
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("pushActiveWarning")
    class PushActiveWarning {

        @Test
        @DisplayName("sends to correct room topic")
        void sendsToCorrectTopic() {
            liveDataService.pushActiveWarning(roomId, sampleWarning());

            assertThat(capture().destination())
                    .isEqualTo("/topic/active-warnings/" + roomId);
        }

        @Test
        @DisplayName("payload is valid JSON containing the warning ID")
        void payloadContainsWarningId() {
            WarningDTO warning = sampleWarning();
            liveDataService.pushActiveWarning(roomId, warning);

            assertThat(capture().payload())
                    .contains(warning.id().toString());
        }

        @Test
        @DisplayName("payload serialises LocalDateTime as ISO string, not timestamp array")
        void payloadUsesIsoDateFormat() {
            liveDataService.pushActiveWarning(roomId, sampleWarning());

            assertThat(capture().payload())
                    .contains("2024-06-15T12:00:00")
                    .doesNotContain("[2024,6");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // pushActiveWarningDepartment
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("pushActiveWarningDepartment")
    class PushActiveWarningDepartment {

        @Test
        @DisplayName("sends to correct department topic")
        void sendsToCorrectTopic() {
            liveDataService.pushActiveWarningDepartment(deptId, sampleWarning());

            assertThat(capture().destination())
                    .isEqualTo("/topic/active-warnings-department/" + deptId);
        }

        @Test
        @DisplayName("payload is empty JSON object")
        void payloadIsEmptyJson() {
            liveDataService.pushActiveWarningDepartment(deptId, sampleWarning());

            assertThat(capture().payload()).isNotEqualTo("{}");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // resolveActiveWarning(UUID) — department overload
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resolveActiveWarning (department)")
    class ResolveActiveWarningDepartment {

        @Test
        @DisplayName("sends to correct department resolve topic")
        void sendsToCorrectTopic() {
            liveDataService.resolveActiveWarningDepartment(deptId, sampleWarning());
            assertThat(capture().destination())
                    .isEqualTo("/topic/resolve-warnings-department/" + deptId);
        }

        @Test
        @DisplayName("payload is empty JSON object")
        void payloadIsEmptyJson() {
            liveDataService.resolveActiveWarningDepartment(deptId, sampleWarning());

            assertThat(capture().payload()).isNotEqualTo("{}");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // resolveActiveWarning(UUID, WarningDTO) — room overload
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resolveActiveWarning (room + WarningDTO)")
    class ResolveActiveWarningRoom {

        @Test
        @DisplayName("sends to correct room resolve topic")
        void sendsToCorrectTopic() {
            liveDataService.resolveActiveWarning(roomId, sampleWarning());

            assertThat(capture().destination())
                    .isEqualTo("/topic/resolve-warnings/" + roomId);
        }

        @Test
        @DisplayName("payload contains the warning ID")
        void payloadContainsWarningId() {
            WarningDTO warning = sampleWarning();
            liveDataService.resolveActiveWarning(roomId, warning);

            assertThat(capture().payload())
                    .contains(warning.id().toString());
        }

        @Test
        @DisplayName("payload serialises LocalDateTime as ISO string, not timestamp array")
        void payloadUsesIsoDateFormat() {
            liveDataService.resolveActiveWarning(roomId, sampleWarning());

            assertThat(capture().payload())
                    .contains("2024-06-15T12:00:00")
                    .doesNotContain("[2024,6");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // pushLiveClimateData
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("pushLiveClimateData")
    class PushLiveClimateData {

        @Test
        @DisplayName("sends to correct room climate topic")
        void sendsToCorrectTopic() {
            liveDataService.pushLiveClimateData(roomId, sampleClimateData());

            assertThat(capture().destination())
                    .isEqualTo("/topic/climate-data/" + roomId);
        }

        @Test
        @DisplayName("payload serialises LocalDateTime as ISO string, not timestamp array")
        void payloadUsesIsoDateFormat() {
            liveDataService.pushLiveClimateData(roomId, sampleClimateData());

            assertThat(capture().payload())
                    .contains("2024-06-15T12:00:00")
                    .doesNotContain("[2024,6");
        }
    }
}