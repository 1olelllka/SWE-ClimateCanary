package at.qe.skeleton.services.impl;

import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.model.Absence;
import at.qe.skeleton.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public void sendWarningEmail(String to, String recipientName, WarningDTO warning, String roomNumber) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject("Climate Warning – Room " + roomNumber);

            StringBuilder body = new StringBuilder();
            body.append("Hello ").append(recipientName).append(",\n\n");
            body.append("A climate warning has been triggered in room ").append(roomNumber).append(".\n\n");
            body.append("Type:     ").append(warning.measurementType()).append("\n");
            body.append("Severity: ").append(warning.status()).append("\n");
            body.append("Measured: ").append(warning.triggeredValue()).append("\n");
            body.append("Limit:    ").append(warning.activeLimitAtTime()).append("\n");
            if (warning.message() != null && !warning.message().isBlank()) {
                body.append("Details:  ").append(warning.message()).append("\n");
            }
            if (warning.tip() != null && !warning.tip().isBlank()) {
                body.append("\nTip: ").append(warning.tip()).append("\n");
            }
            body.append("\nThis is an automated notification from Climate Canary.");

            message.setText(body.toString());
            mailSender.send(message);
            log.debug("Warning email sent to {}", to);
        } catch (Exception e) {
            log.warn("Failed to send warning email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendAbsenceStatusEmail(String to, String recipientName, Absence absence) {
        try {
            String statusLabel = capitalize(absence.getStatus().name().toLowerCase());
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject("Absence Request " + statusLabel);

            StringBuilder body = new StringBuilder();
            body.append("Hello ").append(recipientName).append(",\n\n");
            body.append("Your absence request has been ").append(statusLabel.toLowerCase()).append(".\n\n");
            body.append("Type:  ").append(absence.getTypeOfAbsence()).append("\n");
            body.append("From:  ").append(absence.getStartDate().toLocalDate()).append("\n");
            body.append("To:    ").append(absence.getEndDate().toLocalDate()).append("\n");
            if (absence.getComment() != null && !absence.getComment().isBlank()) {
                body.append("Note:  ").append(absence.getComment()).append("\n");
            }
            body.append("\nThis is an automated notification from Climate Canary.");

            message.setText(body.toString());
            mailSender.send(message);
            log.debug("Absence status email sent to {}", to);
        } catch (Exception e) {
            log.warn("Failed to send absence email to {}: {}", to, e.getMessage());
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
