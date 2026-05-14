package at.qe.skeleton.commands;

import at.qe.skeleton.model.RaspberryPi;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface Command {
    ResponseEntity<Void> execute();
    RaspberryPi getRaspberry();
    UUID getRaspberryId();
    int getAttempts();
    void incrementAttempts();
    void resetAttempts();
}
