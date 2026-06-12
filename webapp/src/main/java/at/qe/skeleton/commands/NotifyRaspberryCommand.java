package at.qe.skeleton.commands;

import at.qe.skeleton.dtos.ConfigRequestDTO;
import at.qe.skeleton.dtos.LimitChangeNotificationDTO;
import at.qe.skeleton.dtos.OccupancyDTO;
import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.RaspberryPi;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;
import java.net.URI;
import java.util.UUID;

/**
 * Command that delivers a notification to a specific Raspberry Pi over HTTP.
 * Depending on which constructor was used, {@link #execute()} dispatches one of
 * four payload types:
 * <ul>
 *   <li>{@link LimitChangeNotificationDTO} — sensor limit update</li>
 *   <li>{@link OccupancyDTO} — room occupancy change</li>
 *   <li>{@link ConfigRequestDTO} — configuration check request</li>
 *   <li>{@link StateChangeNotificationDTO} — sensor state change (with optional
 *       read/write UUID pair for connection retry)</li>
 * </ul>
 * Implements {@link Serializable} to allow the command to be stored or transferred
 * if needed.
 */
public class NotifyRaspberryCommand implements Command, Serializable {

    private final NotificationClient client;
    @Getter
    private StateChangeNotificationDTO dto;
    private UUID readId;
    private UUID writeId;
    private final String piIp;
    private final int port;
    private final UUID piId;
    private LimitChangeNotificationDTO limitDto = null;
    private OccupancyDTO occupancyDTO = null;
    private ConfigRequestDTO configRequestDTO = null;
    private int attempts = 0;

    /**
     * Creates a command for a sensor state-change notification with an associated
     * read/write sensor UUID pair.
     *
     * @param dto     the state-change payload
     * @param readId  UUID of the read-characteristic of the sensor
     * @param writeId UUID of the write-characteristic of the sensor
     * @param pi      the target Raspberry Pi
     * @param client  Feign client used to perform the HTTP call
     */
    public NotifyRaspberryCommand(StateChangeNotificationDTO dto,
                                  UUID readId,
                                  UUID writeId,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.dto = dto;
        this.readId = readId;
        this.writeId = writeId;
        this.client = client;
        this.port = pi.getPort();
        this.piIp = pi.getIp();
        this.piId = pi.getId();
    }

    /**
     * Creates a command for a sensor limit-change notification.
     *
     * @param limitDto the limit-change payload
     * @param pi       the target Raspberry Pi
     * @param client   Feign client used to perform the HTTP call
     */
    public NotifyRaspberryCommand(LimitChangeNotificationDTO limitDto,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.limitDto = limitDto;
        this.client = client;
        this.port = pi.getPort();
        this.piIp = pi.getIp();
        this.piId = pi.getId();
    }

    /**
     * Creates a command for an occupancy-change notification.
     *
     * @param occupancyDTO the occupancy payload
     * @param pi           the target Raspberry Pi
     * @param client       Feign client used to perform the HTTP call
     */
    public NotifyRaspberryCommand(OccupancyDTO occupancyDTO,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.occupancyDTO = occupancyDTO;
        this.port = pi.getPort();
        this.piIp = pi.getIp();
        this.client = client;
        this.piId = pi.getId();
    }

    /**
     * Creates a command that asks the Raspberry Pi to re-check its configuration.
     *
     * @param requestDTO the configuration-check request payload
     * @param pi         the target Raspberry Pi
     * @param client     Feign client used to perform the HTTP call
     */
    public NotifyRaspberryCommand(ConfigRequestDTO requestDTO,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.configRequestDTO = requestDTO;
        this.port = pi.getPort();
        this.piIp = pi.getIp();
        this.client = client;
        this.piId = pi.getId();
    }

    /**
     * Creates a command that retries the sensor connection using only the read/write
     * UUID pair (no state-change DTO).
     *
     * @param readId  UUID of the read-characteristic of the sensor
     * @param writeId UUID of the write-characteristic of the sensor
     * @param pi      the target Raspberry Pi
     * @param client  Feign client used to perform the HTTP call
     */
    public NotifyRaspberryCommand(UUID readId, UUID writeId,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.writeId = writeId;
        this.readId = readId;
        this.port = pi.getPort();
        this.piIp = pi.getIp();
        this.client = client;
        this.piId = pi.getId();
    }

    /**
     * Dispatches the appropriate HTTP notification to the target Raspberry Pi based on
     * which payload field is set. Evaluation order: limit change → occupancy → config
     * request → sensor state change (with optional connection retry if {@code dto} is
     * {@code null}).
     *
     * @return the HTTP response from the Raspberry Pi
     */
    @Override
    public ResponseEntity<Void> execute() {
        ResponseEntity<Void> response;
        URI piUri = URI.create("http://" + this.piIp + ":" + this.port);
        if (limitDto != null) {
            response = client.notifyRaspberryAboutLimitsChange(piUri, this.limitDto);
            return response;
        }
        if (occupancyDTO != null) {
            response = client.notifyAboutOccupancyChanges(piUri, occupancyDTO);
            return response;
        }
        if (configRequestDTO != null) {
            response = client.requestRaspberryToCheckConfig(piUri, configRequestDTO);
            return response;
        }
        if (writeId == null || readId == null) {
            response = client.notifyRaspberryAboutSensorChanges(piUri, dto, null);
        } else {
            if (dto == null) {
                response = client.retrySensorConnection(piUri, new UUID[]{this.readId, this.writeId});
            } else
                response = client.notifyRaspberryAboutSensorChanges(piUri, dto, new UUID[]{this.readId, this.writeId});
        }
        return response;
    }

    /**
     * Returns the ID of the Raspberry Pi this command targets.
     *
     * @return the Raspberry Pi UUID
     */
    @Override
    public UUID getRaspberryId() {
        return this.piId;
    }

    /**
     * Returns the number of delivery attempts made so far.
     *
     * @return current attempt count
     */
    @Override
    public int getAttempts() {
        return this.attempts;
    }

    /**
     * Increments the delivery attempt counter by one.
     */
    @Override
    public void incrementAttempts() {
        this.attempts += 1;
    }

    /**
     * Resets the delivery attempt counter to zero.
     */
    @Override
    public void resetAttempts() {
        this.attempts = 0;
    }

}