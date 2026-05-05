package at.qe.skeleton.configs;

import at.qe.skeleton.model.Permission;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring configuration for web security.
 * <p>
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by Innsbruck University.
 */

@Configuration
public class WebSecurityConfig {

        private final Environment environment;

        @Value("${cors.allowed-origins}")
        private String allowedOrigins;
        @Value("${cors.allowed-methods}")
        private String allowedMethods;
        @Value("${cors.allowed-headers}")
        private String allowedHeaders;
        @Value("${cors.allow-credentials}")
        private boolean allowCredentials;
        @Value("${cors.max-age}")
        private long maxAge;

        public WebSecurityConfig(Environment environment) {
                this.environment = environment;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
                        throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, TokenAuthenticationFilter tokenAuthenticationFilter)
                        throws Exception {

                try {

                        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                        .csrf(AbstractHttpConfigurer::disable).headers(
                                                        headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin)) // needed
                                                                                                                         // for
                                                                                                                         // H2
                                                                                                                         // console
                                        // backend endpoints we want to handle here
                                        .securityMatcher("/api/**", "/h2-console/**")
                                        .authorizeHttpRequests(authorize -> authorize
                                                        .requestMatchers("/h2-console/**").access(devOnly())
                                                        .requestMatchers("/api/login/**").permitAll()
                                                        .requestMatchers("/api/logout").authenticated()
                                                        .requestMatchers("/api-docs/**")
                                                        .permitAll()
                                                        .requestMatchers("/swagger-ui/**")
                                                        .permitAll()
                                                        .requestMatchers("/api/users/me/absences").hasAuthority(Permission.CAN_MANAGE_OWN_ABSENCE.name())
                                                        .requestMatchers("/api/users/me/department/rooms").hasAuthority(Permission.CAN_VIEW_OWN_SHARED_CLIMATE.name())
                                                        .requestMatchers("/api/users/me").authenticated()
                                                        .requestMatchers("/api/users/**").hasAuthority(Permission.CAN_MANAGE_USERS.name())
                                                        .requestMatchers("/api/roles/**").hasAuthority(Permission.CAN_MANAGE_USERS.name())
                                                        .requestMatchers("/api/rooms/*/current-climate", "/api/rooms/*/overtime", "/api/rooms/*/climate-history", "/api/rooms/*/limits", "/api/rooms/*/violations").authenticated()
                                                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/warnings").authenticated()
                                                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/measurements").permitAll()
                                                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/warnings").permitAll()
                                                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/warnings/*/status", "/api/warnings/*/resolve").permitAll()
                                                        .requestMatchers("/api/buildings/**", "/api/departments/**", "/api/rooms/**").hasAuthority(Permission.CAN_MANAGE_BUILDING_STRUCTURE.name())
                                                        .requestMatchers("/api/sensor-stations/**").hasAuthority(Permission.CAN_MANAGE_DEVICES.name())
                                                        .requestMatchers("/api/**").authenticated()
                                                        .anyRequest().authenticated())
                                        // Add the token authentication filter before the
                                        // UsernamePasswordAuthenticationFilter
                                        .addFilterBefore(tokenAuthenticationFilter,
                                                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)

                                        .sessionManagement(
                                                        // no session creation, we use JWT
                                                        session -> session.sessionCreationPolicy(
                                                                        SessionCreationPolicy.STATELESS))
                                        .exceptionHandling(exception -> exception.accessDeniedHandler(
                                                        (request, response, accessDeniedException) -> response
                                                                        .setStatus(
                                                                                        HttpStatus.FORBIDDEN.value()))
                                                        .authenticationEntryPoint(
                                                                        (request, response, authException) -> response
                                                                                        .setStatus(
                                                                                                        HttpStatus.UNAUTHORIZED
                                                                                                                        .value())))
                                .logout(customizer -> {
                                        customizer.logoutUrl("/api/logout")
                                                .clearAuthentication(true)
                                                .logoutSuccessHandler((request, response, authentication) -> {
                                                        if (authentication == null) {
                                                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        } else {
                                                                response.setStatus(HttpServletResponse.SC_OK);
                                                        }
                                                });
                                });

                        return http.build();
                } catch (Exception ex) {
                        throw new BeanCreationException("Wrong spring security configuration", ex);
                }
        }

        private AuthorizationManager<RequestAuthorizationContext> devOnly() {
                return (authentication, context) -> new AuthorizationDecision(environment.matchesProfiles("dev"));
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                List<String> originsList = Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim).filter(s -> !s.isBlank()).toList();
                List<String> methodsList = Arrays.stream(allowedMethods.split(",")).map(String::trim).toList();
                List<String> headersList = Arrays.stream(allowedHeaders.split(",")).map(String::trim).toList();

                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOriginPatterns(originsList);
                config.setAllowedMethods(methodsList);
                config.setAllowedHeaders(headersList);
                config.setAllowCredentials(allowCredentials);
                config.setMaxAge(maxAge);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }

}
