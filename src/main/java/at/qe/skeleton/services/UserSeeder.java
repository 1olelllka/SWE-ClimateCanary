package at.qe.skeleton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
@ConditionalOnProperty(name = "app.seeder.user.enabled", havingValue = "true", matchIfMissing = true)
public class UserSeeder implements ApplicationRunner {

    private UserSeederService service;

    @Autowired
    public UserSeeder(UserSeederService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        service.seed();
    }
}
