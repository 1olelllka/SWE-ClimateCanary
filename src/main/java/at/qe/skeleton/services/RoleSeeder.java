package at.qe.skeleton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class RoleSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private RoleSeederService service;

    @Autowired
    public RoleSeeder(RoleSeederService service) {
        this.service = service;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        service.loadRoles();
    }

}
