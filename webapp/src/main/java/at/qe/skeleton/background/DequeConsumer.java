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

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class DequeConsumer {

    public static final int MAX_ATTEMPTS = 5;

    private volatile boolean running = true;
    private Thread consumerThread;
    private AtomicInteger attempts = new AtomicInteger(0);

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
            log.error("Exception during execute: {}", e.getMessage(), e);
            handleFailure(command);
        }
    }

    public void handleFailure(Command command) {
        AtomicInteger attempts = this.attempts;
        log.warn("Failed attempt {} for Pi [{}:{}]", attempts.get(), command.getRaspberry().getIp(), command.getRaspberry().getPort());
        if (attempts.get() < MAX_ATTEMPTS) {
            CommandDeque.addFirst(command);
            this.attempts.incrementAndGet();
        } else {
            log.error("Failed to communicate with Raspberry Pi [{}:{}]. Setting to offline...", command.getRaspberry().getIp(), command.getRaspberry().getPort());
            command.getRaspberry().setStatus(DeviceStatus.OFFLINE);
            raspberryPiRepository.save(command.getRaspberry());
            this.attempts.set(0);
        }
    }
}