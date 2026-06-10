package at.qe.skeleton.services;

import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.model.Absence;

public interface EmailService {
    void sendWarningEmail(String to, String recipientName, WarningDTO warning, String roomNumber);
    void sendAbsenceStatusEmail(String to, String recipientName, Absence absence);
}
