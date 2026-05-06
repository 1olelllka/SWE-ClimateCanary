package at.qe.skeleton.feign;

import at.qe.skeleton.dtos.ConfigRequestDTO;
import at.qe.skeleton.dtos.LimitChangeNotificationDTO;
import at.qe.skeleton.dtos.OccupancyDTO;
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
    @PostMapping("/api/sensors")
    ResponseEntity<Void> notifyRaspberryAboutSensorChanges(URI baseUrl,
                                                           @RequestBody StateChangeNotificationDTO notification,
                                                           @RequestParam(required = false) UUID[] sensorIds);
    @PostMapping("/api/config")
    ResponseEntity<Void> requestRaspberryToCheckConfig(URI baseUrl,
                                                       @RequestBody ConfigRequestDTO configRequestDTO);

    @PostMapping("/api/limits")
    ResponseEntity<Void> notifyRaspberryAboutLimitsChange(URI baseUrl,
                                                          @RequestBody LimitChangeNotificationDTO dto);

    @PostMapping("/api/occupancy")
    ResponseEntity<Void> notifyAboutOccupancyChanges(URI baseUrl,
                                                     @RequestBody OccupancyDTO dto);

    @PostMapping("/api/retry-sensor")
    ResponseEntity<Void> retrySensorConnection(URI baseUrl,
                                               @RequestParam UUID[] sensorIds);
}
