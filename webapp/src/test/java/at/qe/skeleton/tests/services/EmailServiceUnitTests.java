package at.qe.skeleton.tests.services;

import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.model.*;
import at.qe.skeleton.services.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl")
class EmailServiceUnitTests {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    private static final String FROM    = "noreply@climatecanary.test";
    private static final String TO      = "user@example.com";
    private static final String NAME    = "Alice";
    private static final String ROOM    = "303";

    @BeforeEach
    void injectFromAddress() {
        ReflectionTestUtils.setField(emailService, "fromAddress", FROM);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private SimpleMailMessage captureWarningMessage(WarningDTO warning) {
        emailService.sendWarningEmail(TO, NAME, warning, ROOM);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        return captor.getValue();
    }

    private SimpleMailMessage captureAbsenceMessage(Absence absence) {
        emailService.sendAbsenceStatusEmail(TO, NAME, absence);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        return captor.getValue();
    }

    private WarningDTO warningWith(String message, String tip) {
        return new WarningDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                MeasurementType.CO2,
                WarningStatus.RED,
                message,
                1450.0,      // triggeredValue
                1000.0,      // activeLimitAtTime
                LocalDateTime.now(),
                null,
                tip,
                true
        );
    }

    private Absence absenceWith(AbsenceStatus status, String comment) {
        Absence a = new Absence();
        a.setStatus(status);
        a.setTypeOfAbsence(AbsenceType.VACATION);
        a.setStartDate(LocalDateTime.of(2025, 6, 1, 0, 0));
        a.setEndDate(LocalDateTime.of(2025, 6, 7, 0, 0));
        a.setComment(comment);
        return a;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // sendWarningEmail
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sendWarningEmail")
    class SendWarningEmail {

        @Test
        @DisplayName("sends exactly one message")
        void sendsExactlyOneMessage() {
            emailService.sendWarningEmail(TO, NAME, warningWith(null, null), ROOM);
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("sets From, To, and Subject correctly")
        void setsEnvelopeFields() {
            SimpleMailMessage msg = captureWarningMessage(warningWith(null, null));

            assertThat(msg.getFrom()).isEqualTo(FROM);
            assertThat(msg.getTo()).containsExactly(TO);
            assertThat(msg.getSubject()).isEqualTo("Climate Warning – Room " + ROOM);
        }

        @Test
        @DisplayName("body contains recipient name, room number, and all warning fields")
        void bodyContainsCoreWarningFields() {
            WarningDTO warning = warningWith(null, null);
            SimpleMailMessage msg = captureWarningMessage(warning);
            String body = msg.getText();

            assertThat(body).contains("Hello " + NAME);
            assertThat(body).contains(ROOM);
            assertThat(body).contains(warning.measurementType().name());
            assertThat(body).contains(warning.status().name());
            assertThat(body).contains(String.valueOf(warning.triggeredValue()));
            assertThat(body).contains(String.valueOf(warning.activeLimitAtTime()));
        }

        @Test
        @DisplayName("body includes Details line when message is non-blank")
        void bodyIncludesDetailsWhenPresent() {
            SimpleMailMessage msg = captureWarningMessage(warningWith("Ventilate now", null));

            assertThat(msg.getText()).contains("Details:").contains("Ventilate now");
        }

        @Test
        @DisplayName("body omits Details line when message is null")
        void bodyOmitsDetailsWhenNull() {
            SimpleMailMessage msg = captureWarningMessage(warningWith(null, null));

            assertThat(msg.getText()).doesNotContain("Details:");
        }

        @Test
        @DisplayName("body omits Details line when message is blank")
        void bodyOmitsDetailsWhenBlank() {
            SimpleMailMessage msg = captureWarningMessage(warningWith("   ", null));

            assertThat(msg.getText()).doesNotContain("Details:");
        }

        @Test
        @DisplayName("body includes Tip line when tip is non-blank")
        void bodyIncludesTipWhenPresent() {
            SimpleMailMessage msg = captureWarningMessage(warningWith(null, "Open a window."));

            assertThat(msg.getText()).contains("Tip:").contains("Open a window.");
        }

        @Test
        @DisplayName("body omits Tip line when tip is null")
        void bodyOmitsTipWhenNull() {
            SimpleMailMessage msg = captureWarningMessage(warningWith(null, null));

            assertThat(msg.getText()).doesNotContain("Tip:");
        }

        @Test
        @DisplayName("body omits Tip line when tip is blank")
        void bodyOmitsTipWhenBlank() {
            SimpleMailMessage msg = captureWarningMessage(warningWith(null, "  "));

            assertThat(msg.getText()).doesNotContain("Tip:");
        }

        @Test
        @DisplayName("body includes both Details and Tip when both are present")
        void bodyIncludesBothDetailsAndTip() {
            SimpleMailMessage msg = captureWarningMessage(warningWith("High CO2", "Open a window."));
            String body = msg.getText();

            assertThat(body).contains("Details:").contains("High CO2");
            assertThat(body).contains("Tip:").contains("Open a window.");
        }

        @Test
        @DisplayName("body ends with automated notification footer")
        void bodyContainsFooter() {
            SimpleMailMessage msg = captureWarningMessage(warningWith(null, null));

            assertThat(msg.getText()).contains("automated notification from Climate Canary");
        }

        @Test
        @DisplayName("does not propagate exception when mailSender throws")
        void swallowsMailSenderException() {
            doThrow(new RuntimeException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

            // must not throw
            assertDoesNotThrow(() -> emailService.sendWarningEmail(TO, NAME, warningWith(null, null), ROOM));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // sendAbsenceStatusEmail
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sendAbsenceStatusEmail")
    class SendAbsenceStatusEmail {

        @Test
        @DisplayName("sends exactly one message")
        void sendsExactlyOneMessage() {
            emailService.sendAbsenceStatusEmail(TO, NAME, absenceWith(AbsenceStatus.APPROVED, null));
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("sets From, To correctly")
        void setsEnvelopeFields() {
            SimpleMailMessage msg = captureAbsenceMessage(absenceWith(AbsenceStatus.APPROVED, null));

            assertThat(msg.getFrom()).isEqualTo(FROM);
            assertThat(msg.getTo()).containsExactly(TO);
        }

        @Test
        @DisplayName("subject contains capitalized APPROVED status")
        void subjectContainsCapitalizedApprovedStatus() {
            SimpleMailMessage msg = captureAbsenceMessage(absenceWith(AbsenceStatus.APPROVED, null));

            assertThat(msg.getSubject()).isEqualTo("Absence Request Approved");
        }

        @Test
        @DisplayName("subject contains capitalized REJECTED status")
        void subjectContainsCapitalizedRejectedStatus() {
            SimpleMailMessage msg = captureAbsenceMessage(absenceWith(AbsenceStatus.REJECTED, null));

            assertThat(msg.getSubject()).isEqualTo("Absence Request Rejected");
        }

        @Test
        @DisplayName("subject contains capitalized PENDING status")
        void subjectContainsCapitalizedPendingStatus() {
            SimpleMailMessage msg = captureAbsenceMessage(absenceWith(AbsenceStatus.PENDING, null));

            assertThat(msg.getSubject()).isEqualTo("Absence Request Pending");
        }

        @Test
        @DisplayName("body contains recipient name and status in lowercase")
        void bodyContainsNameAndStatusLowercase() {
            SimpleMailMessage msg = captureAbsenceMessage(absenceWith(AbsenceStatus.APPROVED, null));
            String body = msg.getText();

            assertThat(body).contains("Hello " + NAME);
            assertThat(body).contains("approved"); // lowercase in body sentence
        }

        @Test
        @DisplayName("body contains absence type, start date and end date")
        void bodyContainsAbsenceDateRange() {
            Absence absence = absenceWith(AbsenceStatus.APPROVED, null);
            SimpleMailMessage msg = captureAbsenceMessage(absence);
            String body = msg.getText();

            assertThat(body).contains(absence.getTypeOfAbsence().toString());
            assertThat(body).contains(absence.getStartDate().toLocalDate().toString());
            assertThat(body).contains(absence.getEndDate().toLocalDate().toString());
        }

        @Test
        @DisplayName("body includes Note line when comment is non-blank")
        void bodyIncludesNoteWhenPresent() {
            SimpleMailMessage msg = captureAbsenceMessage(absenceWith(AbsenceStatus.APPROVED, "Medical appointment"));
            String body = msg.getText();

            assertThat(body).contains("Note:").contains("Medical appointment");
        }

        @Test
        @DisplayName("body omits Note line when comment is null")
        void bodyOmitsNoteWhenNull() {
            SimpleMailMessage msg = captureAbsenceMessage(absenceWith(AbsenceStatus.APPROVED, null));

            assertThat(msg.getText()).doesNotContain("Note:");
        }

        @Test
        @DisplayName("body omits Note line when comment is blank")
        void bodyOmitsNoteWhenBlank() {
            SimpleMailMessage msg = captureAbsenceMessage(absenceWith(AbsenceStatus.APPROVED, "   "));

            assertThat(msg.getText()).doesNotContain("Note:");
        }

        @Test
        @DisplayName("body ends with automated notification footer")
        void bodyContainsFooter() {
            SimpleMailMessage msg = captureAbsenceMessage(absenceWith(AbsenceStatus.APPROVED, null));

            assertThat(msg.getText()).contains("automated notification from Climate Canary");
        }

        @Test
        @DisplayName("does not propagate exception when mailSender throws")
        void swallowsMailSenderException() {
            doThrow(new RuntimeException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

            // must not throw
            assertDoesNotThrow(() -> emailService.sendAbsenceStatusEmail(TO, NAME, absenceWith(AbsenceStatus.APPROVED, null)));
        }
    }
}