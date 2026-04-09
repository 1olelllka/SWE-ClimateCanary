package at.qe.skeleton.feign;

import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="notification-raspberry-client", url = "http://172.20.10.3:8080", dismiss404 = true)
public interface NotificationClient {
    @PostMapping("/api/notify")
    ResponseEntity<Void> notifyRaspberryAboutChanges(@RequestBody StateChangeNotificationDTO notification);
}
