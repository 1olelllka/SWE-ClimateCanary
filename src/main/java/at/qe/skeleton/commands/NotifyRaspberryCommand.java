package at.qe.skeleton.commands;

import at.qe.skeleton.dtos.*;
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
    private final RaspberryPi pi;
    private LimitChangeNotificationDTO limitDto = null;
    private OccupancyDTO occupancyDTO = null;
    private ConfigRequestDTO configRequestDTO = null;


    public NotifyRaspberryCommand(StateChangeNotificationDTO dto,
                                  UUID readId,
                                  UUID writeId,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.dto = dto;
        this.readId = readId;
        this.writeId = writeId;
        this.pi = pi;
        this.client = client;
    }

    public NotifyRaspberryCommand(LimitChangeNotificationDTO limitDto,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.limitDto = limitDto;
        this.pi = pi;
        this.client = client;
    }

    public NotifyRaspberryCommand(OccupancyDTO occupancyDTO,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.occupancyDTO = occupancyDTO;
        this.pi = pi;
        this.client = client;
    }

    public NotifyRaspberryCommand(ConfigRequestDTO requestDTO,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.configRequestDTO = requestDTO;
        this.pi = pi;
        this.client = client;
    }

    public NotifyRaspberryCommand(UUID readId, UUID writeId,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.writeId = writeId;
        this.readId = readId;
        this.pi = pi;
        this.client = client;
    }

    @Override
    public ResponseEntity<Void> execute() {
        ResponseEntity<Void> response;
        URI piUri = URI.create("http://" + pi.getIp() + ":" + pi.getPort());
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
    public RaspberryPi getRaspberry() {
        return this.pi;
    }

}
