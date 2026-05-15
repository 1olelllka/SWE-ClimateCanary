package at.qe.skeleton.commands;

import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface Command {
    ResponseEntity<Void> execute();
    UUID getRaspberryId();
    int getAttempts();
    void incrementAttempts();
    void resetAttempts();
}
