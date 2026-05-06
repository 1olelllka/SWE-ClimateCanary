package at.qe.skeleton.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RedisHash("UserClockStatus")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserClockStatus {
    @Id
    @Indexed
    private UUID userId;
    private boolean clockedIn; // if true – one more clock in isn't

    @TimeToLive
    public long getTimeToLive() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().atStartOfDay().plusDays(1);
        return ChronoUnit.SECONDS.between(now, midnight);
    }
}
