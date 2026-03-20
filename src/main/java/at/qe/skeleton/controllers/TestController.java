package at.qe.skeleton.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.Map;


@RestController
@RequestMapping("/api/test")
public class TestController {


    @GetMapping("/hello")
    public Map<String, String> sayHello() {
        return Map.of("message", "Webapp Says Hello from Backend!");
    }
}

