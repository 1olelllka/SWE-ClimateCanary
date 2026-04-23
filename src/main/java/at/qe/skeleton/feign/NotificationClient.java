package at.qe.skeleton.feign;

import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.UUID;

@FeignClient(name="notification-raspberry-client", url = "http://dummy-url.com")
public interface NotificationClient {
    @PostMapping("/api/notify")
    ResponseEntity<Void> notifyRaspberryAboutChanges(URI baseUrl,
                                                     @RequestBody StateChangeNotificationDTO notification,
                                                     @RequestParam UUID[] sensorIds);
    @PostMapping("/api/notify")
    ResponseEntity<Void> notifyRaspberryAboutChanges(URI baseUrl,
                                                     @RequestBody StateChangeNotificationDTO notification);
}
