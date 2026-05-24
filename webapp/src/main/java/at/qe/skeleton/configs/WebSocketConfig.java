package at.qe.skeleton.configs;

import at.qe.skeleton.model.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    private final WebSocketInterceptor authWebSocketInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/active-events")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authWebSocketInterceptor);
    }

    @Bean
    AuthorizationManager<Message<?>> messageAuthorizationManager() {
        MessageMatcherDelegatingAuthorizationManager.Builder messages =
                MessageMatcherDelegatingAuthorizationManager.builder();
        messages
                .simpTypeMatchers(SimpMessageType.CONNECT)
                .authenticated()
                .simpSubscribeDestMatchers("/topic/resolve-warnings-department/*",
                        "topic/active-warnings-department")
                .hasAuthority(Permission.CAN_VIEW_VIOLATIONS_PER_DEPARTMENT.name())
                .simpSubscribeDestMatchers("/topic/**")
                .hasAnyAuthority(
                        Permission.CAN_VIEW_OWN_DEPARTMENT_WARNINGS.name(),
                        Permission.CAN_VIEW_ALL_BUILDINGS.name(),
                        Permission.CAN_VIEW_OWN_OFFICE_CLIMATE.name(),
                        Permission.CAN_VIEW_OWN_SHARED_CLIMATE.name(),
                        Permission.CAN_VIEW_OWN_OFFICE_WARNINGS.name(),
                        Permission.CAN_VIEW_OWN_DEPARTMENT_MEASURES.name())
                .anyMessage()
                .permitAll();
        return messages.build();
    }
}