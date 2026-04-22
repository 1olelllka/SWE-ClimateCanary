package at.qe.skeleton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@ConditionalOnProperty(name = "app.seeder.user.enabled", havingValue = "true", matchIfMissing = true)
public class UserSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private UserSeederService service;

    @Autowired
    public UserSeeder(UserSeederService service) {
        this.service = service;
    }


    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        service.seed();
    }
}
