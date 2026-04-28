package at.qe.skeleton.background;

import at.qe.skeleton.commands.Command;
import at.qe.skeleton.commands.CommandDeque;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@Log
public class DequeConsumer {

    public static final int MAX_ATTEMPTS = 5;

    private volatile boolean running = true;
    private Thread consumerThread;
    private Integer attempts = 0;

    private final RaspberryPiRepository raspberryPiRepository;

    @Autowired
    public DequeConsumer(RaspberryPiRepository raspberryPiRepository) {
        this.raspberryPiRepository = raspberryPiRepository;
    }

    @PostConstruct
    void init() {
        consumerThread = new Thread(this::consume);
        consumerThread.setDaemon(true);
        consumerThread.setName("deque-consumer");
        consumerThread.start();
    }

    @PreDestroy
    void shutdown() {
        running = false;
        consumerThread.interrupt();
    }

    public void consume() {
        log.info("Running deque consumer...");
        while (running) {
            try {
                Command command = CommandDeque.getFirst();
                if (command == null) continue;
                log.info("Caught command: " + command.getClass().getSimpleName());
                processCommand(command);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.info("Consumer interrupted, shutting down...");
                break;
            }
        }
    }

    void processCommand(Command command) {
        try {
            ResponseEntity<Void> response = command.execute();
            if (response.getStatusCode().value() >= 300) {
                handleFailure(command);
            }
            if (response.getStatusCode().value() < 300) {
                RaspberryPi pi = command.getRaspberry();
                pi.setStatus(DeviceStatus.ONLINE);
                raspberryPiRepository.save(pi);
            }
        } catch (Exception e) {
            log.info("Exception during execute: " + e.getMessage());
            handleFailure(command);
        }
    }

    public void handleFailure(Command command) {
        int attempts = this.attempts;
        log.info("Failed attempt " + attempts);
        if (attempts < MAX_ATTEMPTS) {
            CommandDeque.addFirst(command);
            this.attempts += 1;
        } else {
            log.info("Failed to communicate with raspberry pi");
            command.getRaspberry().setStatus(DeviceStatus.OFFLINE);
            raspberryPiRepository.save(command.getRaspberry());
            this.attempts = 0;
        }
    }
}