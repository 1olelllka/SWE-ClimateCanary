/**
 * Spring configuration for web security.
 * <p>
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by Innsbruck University.
 */

package at.qe.skeleton.configs;

import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.UserxService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserxService userService;
    private final JwtConfig jwtConfig;

    @Autowired
    public TokenAuthenticationFilter(JwtTokenProvider tokenProvider, UserxService userService, JwtConfig jwtConfig) {
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.jwtConfig = jwtConfig;
    }

    // This method is called by the filter chain to handle JWT authentication within a request lifecycle.
    @Override
    public void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws ServletException, IOException {
        try {
            getJwtFromRequest(request)
                    .flatMap(tokenProvider::validateTokenAndGetJws)
                    .ifPresent(jws -> {
                        String username = jws.getPayload().getSubject();
                        Userx userDetails = userService.getUserByUsername(username);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        } catch (ExpiredJwtException e) {
            System.err.println("Cannot set user authentication " + e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is expired");
        } catch (MalformedJwtException e) {
            System.err.println("Cannot parse token " + e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is invalid");
        } catch (Exception e) {
            System.err.println("Cannot set user authentication " + e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        }
        chain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the request.
     *
     * @param request the request
     * @return the JWT token if present
     */
    private Optional<String> getJwtFromRequest(HttpServletRequest request) {
        String fullTokenHeader = request.getHeader(jwtConfig.getTokenHeader());
        if (fullTokenHeader != null && fullTokenHeader.startsWith(jwtConfig.getTokenPrefix())) {
            return Optional.of(fullTokenHeader.replace(jwtConfig.getTokenPrefix(), ""));
        }
        return Optional.empty();
    }
}