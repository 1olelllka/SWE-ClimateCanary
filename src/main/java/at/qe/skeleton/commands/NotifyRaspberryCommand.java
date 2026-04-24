package at.qe.skeleton.commands;

import at.qe.skeleton.dtos.LimitChangeNotificationDTO;
import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.dtos.UpdateType;
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
    private LimitChangeNotificationDTO limitDto;


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

    @Override
    public ResponseEntity<Void> execute() {
        ResponseEntity<Void> response;
        URI piUri = URI.create("http://" + pi.getIp() + ":" + pi.getPort());
        if (limitDto != null) {
            response = client.notifyRaspberryAboutLimitsChange(piUri, this.limitDto);
            return response;
        }
        if (writeId == null || readId == null) {
            if (this.dto.updateType() == UpdateType.CONFIG) {
                response = client.notifyRaspberryAboutChanges(piUri, dto, null, pi.getId());
            } else {
                response = client.notifyRaspberryAboutChanges(piUri, dto);
            }
        } else {
            response = client.notifyRaspberryAboutChanges(piUri, dto, new UUID[]{this.readId, this.writeId}, null);
        }
        return response;
    }

    @Override
    public RaspberryPi getRaspberry() {
        return this.pi;
    }

}
