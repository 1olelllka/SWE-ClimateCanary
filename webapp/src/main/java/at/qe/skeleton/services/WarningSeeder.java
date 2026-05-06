package at.qe.skeleton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(6)
@ConditionalOnProperty(name = "app.seeder.warning.enabled", havingValue = "true", matchIfMissing = true)
public class WarningSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private final WarningSeederService service;

    @Autowired
    public WarningSeeder(WarningSeederService service) {
        this.service = service;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        service.seed();
    }
}
