package at.qe.skeleton.commands;

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
    private final StateChangeNotificationDTO dto;
    private final UUID sensorId;
    private final RaspberryPi pi;


    public NotifyRaspberryCommand(StateChangeNotificationDTO dto,
                                  UUID sensorId,
                                  RaspberryPi pi,
                                  NotificationClient client) {
        this.dto = dto;
        this.sensorId = sensorId;
        this.pi = pi;
        this.client = client;
    }

    @Override
    public ResponseEntity<Void> execute() {
        ResponseEntity<Void> response;
        if (sensorId == null) {
            response = client.notifyRaspberryAboutChanges(URI.create(pi.getPort() + ":" + pi.getPort()), dto);
        } else {
            response = client.notifyRaspberryAboutChanges(URI.create(pi.getPort() + ":" + pi.getPort()), dto, sensorId);
        }
        return response;
    }

}
