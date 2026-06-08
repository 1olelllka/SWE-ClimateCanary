package at.qe.skeleton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(9)
@ConditionalOnProperty(name = "app.seeder.warning.enabled", havingValue = "true", matchIfMissing = true)
public class WarningSeeder implements ApplicationRunner {

    private final WarningSeederService service;

    @Autowired
    public WarningSeeder(WarningSeederService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        service.seed();
    }
}
