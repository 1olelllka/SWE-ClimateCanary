package at.qe.skeleton.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring configuration for web security.
 * <p>
 * This class is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */

@Configuration
public class SpaRedirectConfig implements WebMvcConfigurer {

    /*
     * filter out endpoints that should return index.html for SPA
     * i.e. not: API calls, static file, other known server calls (e.g. h2-console)
    */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        registry.addViewController("/{path:^(?!api|authentication|h2-console|swagger-ui|api-docs|v3|static|assets)[^.]*}")
                .setViewName("forward:/");

        registry.addViewController("/{path:^(?!api|authentication|h2-console|swagger-ui|api-docs|v3|static|assets)[^.]*}/**")
                .setViewName("forward:/");
    }
}
