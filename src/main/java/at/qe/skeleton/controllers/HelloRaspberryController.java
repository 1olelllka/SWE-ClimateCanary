package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.RaspberryCommandDTO;
import at.qe.skeleton.dtos.RaspberryTestDTO;
import at.qe.skeleton.dtos.TestInfoDTO;
import at.qe.skeleton.feign.TestConnectionClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloRaspberryController {

    @Autowired
    private TestConnectionClient connectionClient;

    @GetMapping("/test-raspberry")
    public ResponseEntity<RaspberryTestDTO> getRaspberryResponse() {
        ResponseEntity<RaspberryTestDTO> response = connectionClient.postCommandToRaspberry(new RaspberryCommandDTO("turn_on_led"));
        if (response.getStatusCode().value() >= 300) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
        return new ResponseEntity<>(response.getBody(), HttpStatus.OK);
    }

    @PostMapping("/test-info")
    public ResponseEntity<TestInfoDTO> receiveMsg() {
        System.out.println("...API call performed...");
        return new ResponseEntity<>(new TestInfoDTO("Hello World!!!"), HttpStatus.OK);
    }
}
