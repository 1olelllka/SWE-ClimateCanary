package at.qe.skeleton.services;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(7)
@ConditionalOnProperty(name = "app.seeder.department-stats.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DepartmentStatsSeeder implements ApplicationRunner {

    private final DepartmentStatsSeederService service;

    @Override
    public void run(ApplicationArguments args) {
        service.seed();
    }
}
