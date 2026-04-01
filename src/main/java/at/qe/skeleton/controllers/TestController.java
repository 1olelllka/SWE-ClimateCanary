package at.qe.skeleton.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    // u can play with it and try different combinations
    @GetMapping("/test")
    @PreAuthorize("hasAuthority('CAN_MANAGE_DEVICES')")
    public ResponseEntity<String> test() {
        return new ResponseEntity<>("WOW", HttpStatus.OK);
    }

}
