package at.qe.skeleton.background;

import at.qe.skeleton.commands.Command;
import at.qe.skeleton.commands.CommandDeque;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class DequeConsumer {

    public static final int MAX_ATTEMPTS = 5;

    private volatile boolean running = true;
    private Thread consumerThread;

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
        while (running) {
            try {
                Command command = CommandDeque.getFirst();
                if (command == null) continue;
                log.info("Caught raspberry pi command: {}", command.getClass().getSimpleName());
                processCommand(command);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.error("Consumer interrupted, shutting down...");
                break;
            } catch (Exception ex) {
                log.error("Unexpected error processing command: {}", ex.getMessage(), ex);
            }
        }
    }

    void processCommand(Command command) {
        UUID piId = command.getRaspberryId();
        RaspberryPi pi = raspberryPiRepository.findById(piId)
                .orElseThrow(() -> new IllegalStateException("Pi not found: " + piId));
        try {
            ResponseEntity<Void> response = command.execute();

            if (response.getStatusCode().value() >= 300) {
                handleFailure(command, pi);
            } else {
                if (pi.getStatus() == DeviceStatus.OFFLINE) {
                    pi.setStatus(DeviceStatus.ONLINE);
                    raspberryPiRepository.save(pi);
                }
                log.info("Successfully processed the Raspberry Pi command...");
            }
        } catch (Exception e) {
            log.error("Exception during execute: {}", e.getMessage(), e);
            handleFailure(command, pi);
        }
    }

    public void handleFailure(Command command, RaspberryPi pi) {
        int current = command.getAttempts();
        log.warn("Failed attempt {} for Pi [{}:{}]", current, pi.getIp(), pi.getPort());

        if (current < MAX_ATTEMPTS) {
            command.incrementAttempts();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("Retry sleep interrupted: {}", ex.getMessage(), ex);
            }
            CommandDeque.addFirst(command);
        } else {
            log.error("Failed to communicate with Raspberry Pi [{}:{}]. Setting to offline...",
                    pi.getIp(), pi.getPort());
            if (pi.getStatus() == DeviceStatus.ONLINE) {
                pi.setStatus(DeviceStatus.OFFLINE);
                raspberryPiRepository.save(pi);
            }
            command.resetAttempts();
        }
    }
}