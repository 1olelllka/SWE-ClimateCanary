package at.qe.skeleton.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.*;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
public class UserSettings {
    @Id
    @EqualsAndHashCode.Include
    private UUID userId;
    @Builder.Default
    private boolean darkMode = false;
    @Builder.Default
    private boolean fahrenheit = false;
    @Builder.Default
    @Enumerated(value = EnumType.STRING)
    private DateFormat format = DateFormat.DD_MM_YYYY;
    @Builder.Default
    private boolean twelveHourFormat = false;

    private String notificationEmail;
    @Builder.Default
    private boolean emailWarnings = false;
    @Builder.Default
    private boolean emailAbsences = false;

}
