package at.qe.skeleton.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@RedisHash(value="RoomOccupancy")
public class RoomOccupancy implements Serializable {
    @Id
    @Indexed
    UUID roomId;
    Integer peopleCnt;

    @TimeToLive
    public long getTimeToLive() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().atStartOfDay().plusDays(1);
        return ChronoUnit.SECONDS.between(now, midnight);
    }
}