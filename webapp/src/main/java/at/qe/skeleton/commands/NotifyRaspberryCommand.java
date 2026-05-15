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

    public NotifyRaspberryCommand(LimitChangeNotificationDTO limitDto,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.limitDto = limitDto;
        this.client = client;
        this.port = pi.getPort();
        this.piIp = pi.getIp();
        this.piId = pi.getId();
    }

    public NotifyRaspberryCommand(OccupancyDTO occupancyDTO,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.occupancyDTO = occupancyDTO;
        this.port = pi.getPort();
        this.piIp = pi.getIp();
        this.client = client;
        this.piId = pi.getId();
    }

    public NotifyRaspberryCommand(ConfigRequestDTO requestDTO,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.configRequestDTO = requestDTO;
        this.port = pi.getPort();
        this.piIp = pi.getIp();
        this.client = client;
        this.piId = pi.getId();
    }

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

    @Override
    public UUID getRaspberryId() {
        return this.piId;
    }

    @Override
    public int getAttempts() {
        return this.attempts;
    }

    @Override
    public void incrementAttempts() {
        this.attempts += 1;
    }

    @Override
    public void resetAttempts() {
        this.attempts = 0;
    }

}
