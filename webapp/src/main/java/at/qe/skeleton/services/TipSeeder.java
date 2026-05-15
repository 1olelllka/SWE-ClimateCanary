package at.qe.skeleton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class TipSeeder implements ApplicationRunner {

    private final TipSeederService service;

    @Autowired
    public TipSeeder(TipSeederService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        service.seed();
    }
}
