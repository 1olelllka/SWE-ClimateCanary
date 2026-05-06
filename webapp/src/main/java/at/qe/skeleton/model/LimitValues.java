package at.qe.skeleton.model;


import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class LimitValues {

    @Column(nullable = false)
    protected float maxVal;

    @Column(nullable = true)
    protected Float minVal; //boxed Float because field is Optional

    @Column(nullable = false)
    protected int version;

    @Column(nullable = false)
    protected int recoveryThreshold;
}
