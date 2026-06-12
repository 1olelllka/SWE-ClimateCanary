package at.qe.skeleton.configs;

import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.UserxRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * STOMP {@link ChannelInterceptor} that authenticates WebSocket clients on
 * {@code CONNECT} frames. The JWT is extracted from the {@code Authorization: Bearer}
 * header, validated, and — if valid — used to populate the Spring
 * {@link SecurityContextHolder} and set the STOMP session user principal.
 * Subsequent frames on an authenticated session inherit the established principal
 * without re-validation.
 */
@Component
@RequiredArgsConstructor
public class WebSocketInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final UserxRepository userxRepository;

    /**
     * Intercepts every inbound STOMP message. For {@code CONNECT} frames, extracts
     * the {@code Authorization} header and delegates to {@link #auth} to authenticate
     * the connecting client. All other frame types are passed through unchanged.
     *
     * @param message the inbound STOMP message
     * @param channel the message channel
     * @return the original message, possibly with an updated user principal
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command != null && command.equals(StompCommand.CONNECT)) {
            List<String> authHeader = accessor.getNativeHeader("Authorization");
            auth(authHeader, accessor);
        }
        return message;
    }

    /**
     * Validates the JWT found in the given headers and, on success, registers the
     * corresponding {@link Userx} as the authenticated principal in both the
     * {@link SecurityContextHolder} and the STOMP session accessor.
     *
     * @param headers  the raw {@code Authorization} header values from the STOMP frame
     * @param accessor the STOMP header accessor used to set the session user principal
     * @throws IllegalStateException if the token is expired or cannot be parsed
     */
    private void auth(List<String> headers, StompHeaderAccessor accessor) {
        try {
            getJwtFromRequest(headers)
                    .flatMap(tokenProvider::validateTokenAndGetJws)
                    .ifPresent(jws -> {
                        String username = jws.getPayload().getSubject();
                        Userx userDetails = userxRepository.findByUsernameWithRoles(username).orElseThrow(() -> new RuntimeException("User with such name was not found."));
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        accessor.setUser(authentication);
                    });
        } catch (ExpiredJwtException e) {
            throw new IllegalStateException("Cannot set user authentication " + e);
        } catch (MalformedJwtException e) {
            throw new IllegalStateException("Cannot parse token " + e);
        }
    }

    /**
     * Extracts the raw JWT string from the {@code Authorization} header list.
     * Expects the header value to follow the {@code Bearer <token>} scheme.
     *
     * @param headers the raw {@code Authorization} header values
     * @return an {@link Optional} containing the token string, or empty if the header
     *         is absent or does not start with {@code "Bearer "}
     */
    private Optional<String> getJwtFromRequest(List<String> headers) {
        if (headers.isEmpty()) return Optional.empty();
        String fullToken = headers.getFirst();
        if (fullToken.startsWith("Bearer ")) {
            return Optional.of(fullToken.substring(7));
        }
        return Optional.empty();
    }
}