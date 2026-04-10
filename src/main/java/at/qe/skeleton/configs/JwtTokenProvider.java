/**
 * Spring configuration for web security.
 * <p>
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by Innsbruck University.
 * This class uses code from
 * https://github.com/ivangfr/springboot-react-jwt-token/blob/38aa68e8e94927fe6f072169372fdc7fb8067bc6/order-api/src/main/java/com/ivanfranchin/orderapi/security/TokenProvider.java
 */

package at.qe.skeleton.configs;

import at.qe.skeleton.model.Userx;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    @Autowired
    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * Generates a JWT token for the given authentication (i.e., an authenticated user).
     *
     * @param authentication the authentication object
     * @return the generated JWT token
     */
    public String generate(Authentication authentication) {

        // Get the authenticated user, we can do this since the UserxTypes class implements UserDetails
        Userx user = (Userx) authentication.getPrincipal();

        List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        List<String> permissions = user.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority).filter(g -> g.startsWith("CAN_")).toList();
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.getJwtSecret()));

        Instant now = Instant.now();

        //  @formatter:off
        
        // Build the JWT token (see https://github.com/jwtk/jjwt)
        return Jwts.builder()
                .header()
                    .add("type", jwtConfig.getTokenType())
                .and()
                .signWith(key, Jwts.SIG.HS512)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtConfig.getJwtExpirationMs())))
                .id(UUID.randomUUID().toString())
                .issuer(jwtConfig.getTokenIssuer())
                .audience()
                    .add(jwtConfig.getTokenAudience())
                .and()
                .subject(authentication.getName())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("name", user.getFirstName() + " " + user.getLastName())
                .claim("username", user.getUsername()).compact();
        //  @formatter:on
    }

    /**
     * Validates the given JWT token and returns the signed JWT object.
     * @param token the JWT token to validate
     * @return an Optional with the signed JWT object if the token is valid, or an empty Optional otherwise
     */
    public Optional<Jws<Claims>> validateTokenAndGetJws(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.getJwtSecret()));

            Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);

            return Optional.of(jws);

        } catch (ExpiredJwtException exception) {
            System.err.println("Request to parse expired JWT : " + token + " failed : " + exception.getMessage());
        } catch (UnsupportedJwtException exception) {
            System.err.println("Request to parse unsupported JWT : " + token + " failed : " + exception.getMessage());
        } catch (MalformedJwtException exception) {
            System.err.println("Request to parse invalid JWT : " + token + " failed : " + exception.getMessage());
        } catch (SignatureException exception) {
            System.err.println("Request to parse JWT with invalid signature : " + token + " failed : " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            System.err.println("Request to parse empty or null JWT : " + token + " failed : " + exception.getMessage());
        }
        return Optional.empty();
    }

}
