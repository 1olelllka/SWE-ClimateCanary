package at.qe.skeleton.feign;


import at.qe.skeleton.dtos.RaspberryCommandDTO;
import at.qe.skeleton.dtos.RaspberryTestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="raspberry-test-connection", url = "http://172.20.10.3:8080", dismiss404 = true)
public interface TestConnectionClient {
    @PostMapping("/api/send-command")
    ResponseEntity<RaspberryTestDTO> postCommandToRaspberry(@RequestBody RaspberryCommandDTO dto);
}