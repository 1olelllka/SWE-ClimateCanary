package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.RaspberryCommandDTO;
import at.qe.skeleton.dtos.RaspberryTestDTO;
import at.qe.skeleton.feign.TestConnectionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "*")
public class HelloRaspberryController {
    private static final Logger logger = LoggerFactory.getLogger(HelloRaspberryController.class);
    private final TestConnectionClient connectionClient;
    // placeholder for PiMessage (is overwritten as soon as tip received)
    private RaspberryTestDTO latestPiMessage = new RaspberryTestDTO("Warte auf eine Nachricht...");
    public HelloRaspberryController(TestConnectionClient connectionClient) {
        this.connectionClient = connectionClient; // Spring automatically injects this
    }

    // Frontend POSTs a tip -> forwards to Pi
    @PostMapping("/send-to-raspberry")
    public ResponseEntity<Void> sendToPi(@RequestBody RaspberryCommandDTO command) {
        ResponseEntity<RaspberryTestDTO> response = connectionClient.postCommandToRaspberry(command);
        logger.info("Server leitet Nachricht weiter an Pi");
        if (response.getStatusCode().value() >= 300) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    //Pi POSTs its tip here
    @PostMapping("/test-info")
    public ResponseEntity<Void> receiveMsg(@RequestBody RaspberryTestDTO dto) {
        this.latestPiMessage = dto;
        logger.info("Server received from Pi: {}", dto.message());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // Frontend GETs the latest Pi tip
    @GetMapping("/test-info")
    public ResponseEntity<RaspberryTestDTO> getRaspberryResponse(){
        return new ResponseEntity<>(latestPiMessage, HttpStatus.OK); //returns whatever Pi last sent
    }
}
