package at.qe.skeleton.model;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedisData {
    private UUID roomId;
    private int cnt;
}
