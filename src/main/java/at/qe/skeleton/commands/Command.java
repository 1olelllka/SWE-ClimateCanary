package at.qe.skeleton.commands;

import org.springframework.http.ResponseEntity;

public interface Command {
    ResponseEntity<Void> execute();
}
