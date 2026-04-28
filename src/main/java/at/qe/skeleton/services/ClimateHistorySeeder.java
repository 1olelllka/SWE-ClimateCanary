package at.qe.skeleton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
@ConditionalOnProperty(name = "app.seeder.climate-history.enabled", havingValue = "true", matchIfMissing = true)
public class ClimateHistorySeeder implements ApplicationListener<ContextRefreshedEvent> {

    private final ClimateHistorySeederService service;

    @Autowired
    public ClimateHistorySeeder(ClimateHistorySeederService service) {
        this.service = service;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        service.seed();
    }
}
