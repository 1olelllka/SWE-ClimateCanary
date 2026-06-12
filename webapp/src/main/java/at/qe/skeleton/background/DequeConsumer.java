package at.qe.skeleton.background;

import at.qe.skeleton.commands.Command;
import at.qe.skeleton.commands.CommandDeque;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.services.LiveDataService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Background component that continuously drains {@link CommandDeque} and forwards
 * each {@link Command} to the target Raspberry Pi. Failed commands are retried up to
 * {@link #MAX_ATTEMPTS} times before the device is marked {@link DeviceStatus#OFFLINE}.
 * The consumer runs on a dedicated daemon thread started at application startup.
 */
@Component
@Slf4j
public class DequeConsumer {

    /** Maximum number of delivery attempts before a Raspberry Pi is marked offline. */
    public static final int MAX_ATTEMPTS = 5;

    private volatile boolean running = true;
    private Thread consumerThread;

    private final RaspberryPiRepository raspberryPiRepository;
    private final LiveDataService liveDataService;

    @Autowired
    public DequeConsumer(RaspberryPiRepository raspberryPiRepository,
                         LiveDataService liveDataService) {
        this.raspberryPiRepository = raspberryPiRepository;
        this.liveDataService = liveDataService;
    }

    /**
     * Starts the daemon consumer thread after the bean has been constructed.
     */
    @PostConstruct
    void init() {
        consumerThread = new Thread(this::consume);
        consumerThread.setDaemon(true);
        consumerThread.setName("deque-consumer");
        consumerThread.start();
    }

    /**
     * Signals the consumer loop to stop and interrupts the consumer thread gracefully
     * before the bean is destroyed.
     */
    @PreDestroy
    void shutdown() {
        running = false;
        consumerThread.interrupt();
    }

    /**
     * Main loop: blocks on {@link CommandDeque#getFirst()} and delegates each
     * dequeued command to {@link #processCommand(Command)}. Exits on thread
     * interruption or when {@link #running} is set to {@code false}.
     */
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

    /**
     * Executes a single command against its target Raspberry Pi. On success, marks the
     * device {@link DeviceStatus#ONLINE} if it was previously offline. On failure,
     * delegates to {@link #handleFailure(Command, RaspberryPi)}.
     *
     * @param command the command to execute
     * @throws IllegalStateException if the target Raspberry Pi is not found in the database
     */
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
                    liveDataService.pushConnectionStatusRaspberry(pi.getId(), DeviceStatus.ONLINE);
                    raspberryPiRepository.save(pi);
                }
                log.info("Successfully processed the Raspberry Pi command...");
            }
        } catch (Exception e) {
            log.error("Exception during execute: {}", e.getMessage(), e);
            handleFailure(command, pi);
        }
    }

    /**
     * Handles a failed command delivery. If the attempt count is below
     * {@link #MAX_ATTEMPTS}, the command is re-queued at the front of the deque after
     * a short delay. Once all attempts are exhausted the device is marked
     * {@link DeviceStatus#OFFLINE} and the attempt counter is reset.
     *
     * @param command the command that failed
     * @param pi      the Raspberry Pi that could not be reached
     */
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
                liveDataService.pushConnectionStatusRaspberry(pi.getId(), DeviceStatus.OFFLINE);
                raspberryPiRepository.save(pi);
            }
            command.resetAttempts();
        }
    }
}