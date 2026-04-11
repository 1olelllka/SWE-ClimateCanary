package at.qe.skeleton.commands;

import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.feign.NotificationClient;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;
import java.util.UUID;

public class NotifyRaspberryCommand implements Command, Serializable {

    private final NotificationClient client;
    @Getter
    private final StateChangeNotificationDTO dto;
    @Getter
    private final UUID sensorId;

    public NotifyRaspberryCommand(StateChangeNotificationDTO dto,
                                  UUID sensorId,
                                  NotificationClient client) {
        this.dto = dto;
        this.sensorId = sensorId;
        this.client = client;
    }

    @Override
    public ResponseEntity<Void> execute() {
        ResponseEntity<Void> response;
        if (sensorId == null) {
            response = client.notifyRaspberryAboutChanges(dto);
        } else {
            response = client.notifyRaspberryAboutChanges(dto, sensorId);
        }
        return response;
    }

}
