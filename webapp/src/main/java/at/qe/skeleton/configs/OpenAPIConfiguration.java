
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
       server.setDescription("Development");

       Contact myContact = new Contact();
     myContact.setName("SWE Skeleton Team");

       Info information = new Info().title("SWE Skeleton API").version("0.1")
           .description("This is the API documentation for the SWE Skeleton project.")
               .contact(myContact);
       return new OpenAPI().info(information).servers(List.of(server));
   }
}
