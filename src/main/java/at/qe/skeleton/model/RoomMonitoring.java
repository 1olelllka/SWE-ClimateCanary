package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "room_monitoring")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RoomMonitoring {
    @Id
    @EqualsAndHashCode.Include
    private UUID roomId;

    @Column(nullable = false, unique = true)
    private String roomNumber;
    // Devices
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_station_id")
    private SensorStation sensorStation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raspberry_pi_id")
    private RaspberryPi raspberryPi;

    // Related Limits
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "temp_limit_id")
    private TemperatureLimit tempLimit;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pol_limit_id")
    private PollutionLimit polLimit;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "hum_limit_id")
    private HumidityLimit humLimit;

    // Climate Stats
    @OneToMany(mappedBy = "roomMonitoring", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ClimateStats> climateStats = new ArrayList<>();

    // Warnings
    @OneToMany(mappedBy = "roomMonitoring", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Warnings> warnings = new ArrayList<>();
}
