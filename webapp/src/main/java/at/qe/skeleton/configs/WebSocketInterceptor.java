package at.qe.skeleton.configs;

import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.UserxRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
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

@Component
@Log
@RequiredArgsConstructor
public class WebSocketInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final UserxRepository userxRepository;

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

    private Optional<String> getJwtFromRequest(List<String> headers) {
        if (headers.isEmpty()) return Optional.empty();
        String fullToken = headers.getFirst();
        if (fullToken.startsWith("Bearer ")) {
            return Optional.of(fullToken.substring(7));
        }
        return Optional.empty();
    }
}
