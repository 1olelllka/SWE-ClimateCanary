package at.qe.skeleton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seeder.building.enabled", havingValue = "true", matchIfMissing = true)
public class BuildingSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private final BuildingSeederService service;

    @Autowired
    public BuildingSeeder(BuildingSeederService service) {
        this.service = service;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        service.seed();
    }
}
