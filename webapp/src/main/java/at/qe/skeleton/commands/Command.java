package at.qe.skeleton.commands;

import at.qe.skeleton.model.RaspberryPi;
import org.springframework.http.ResponseEntity;

public interface Command {
    ResponseEntity<Void> execute();
    RaspberryPi getRaspberry();
}
