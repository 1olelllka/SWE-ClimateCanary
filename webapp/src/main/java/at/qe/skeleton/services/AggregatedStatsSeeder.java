package at.qe.skeleton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(6)
@ConditionalOnProperty(name = "app.seeder.climate-history.enabled", havingValue = "true", matchIfMissing = true)
public class AggregatedStatsSeeder implements ApplicationRunner {

    private final AggregatedStatsSeederService service;

    @Autowired
    public AggregatedStatsSeeder(AggregatedStatsSeederService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        service.seed();
    }
}
