
package at.qe.skeleton.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfiguration {

   @Bean
   public OpenAPI defineOpenApi() {
       Server server = new Server();
       server.setUrl("http://localhost:8080");
       server.setDescription("Production");

       Contact myContact = new Contact();
       myContact.setName("G1T4 Team");

       Info information = new Info().title("Climate Canary API").version("1.0.0")
           .description("This is the API documentation for the Climate Canary project.")
               .contact(myContact);
       return new OpenAPI().info(information).servers(List.of(server));
   }
}
