package at.qe.skeleton;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring boot application. Execute maven with <code>mvn spring-boot:run</code>
 * to start this web application.
 *
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
*/
@SpringBootApplication
@EnableMethodSecurity(prePostEnabled = true)
@EnableFeignClients
@EnableTransactionManagement
public class Main extends SpringBootServletInitializer {

    public static void main(String[] args) {
            SpringApplication.run(Main.class, args);
    }
}
