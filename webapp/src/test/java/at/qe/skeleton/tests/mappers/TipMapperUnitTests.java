package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.TipDTO;
import at.qe.skeleton.mappers.TipMapper;
import at.qe.skeleton.model.Tip;
import at.qe.skeleton.model.ViolatedSensor;
import at.qe.skeleton.model.ViolationType;
import at.qe.skeleton.model.WarningStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("TipMapper")
class TipMapperUnitTests {

    private TipMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TipMapper();
    }

    private Tip buildTip(UUID id, ViolationType violationType,
                         ViolatedSensor violatedSensor, String message) {
        Tip tip = new Tip();
        tip.setId(id);
        tip.setViolationType(violationType);
        tip.setViolatedSensor(violatedSensor);
        tip.setMsg(message);
        return tip;
    }

    @Nested
    @DisplayName("mapTo(Tip) — Entity to DTO")
    class MapTo {

        @Test
        @DisplayName("maps all fields correctly when warning and roomMonitoring are present")
        void mapsAllFieldsWithWarningAndRoom() {
            UUID tipId = UUID.randomUUID();
            Tip tip = buildTip(tipId, ViolationType.OVER, ViolatedSensor.TEMPERATURE,
                    "Open a window.");

            TipDTO result = mapper.mapTo(tip);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(tipId);
            assertThat(result.violationType()).isEqualTo(ViolationType.OVER);
            assertThat(result.violatedSensor()).isEqualTo(ViolatedSensor.TEMPERATURE);
            assertThat(result.message()).isEqualTo("Open a window.");
        }

        @Test
        @DisplayName("maps all ViolationType values correctly")
        void mapsAllViolationTypes() {
            for (ViolationType type : ViolationType.values()) {
                Tip tip = buildTip(UUID.randomUUID(), type, ViolatedSensor.TEMPERATURE,
                        "Some tip.");

                TipDTO result = mapper.mapTo(tip);

                assertThat(result.violationType()).isEqualTo(type);
            }
        }

        @Test
        @DisplayName("maps all ViolatedSensor values correctly")
        void mapsAllViolatedSensors() {
            for (ViolatedSensor sensor : ViolatedSensor.values()) {
                Tip tip = buildTip(UUID.randomUUID(), ViolationType.OVER, sensor,
                        "Some tip.");

                TipDTO result = mapper.mapTo(tip);

                assertThat(result.violatedSensor()).isEqualTo(sensor);
            }
        }

        @Test
        @DisplayName("throws NullPointerException when entity is null")
        void throwsOnNullEntity() {
            assertThrows(NullPointerException.class, () -> mapper.mapTo(null));
        }
    }

    @Nested
    @DisplayName("mapFrom(TipDTO) — unsupported direction")
    class MapFrom {

        @Test
        @DisplayName("throws UnsupportedOperationException — read DTO is not used for writes")
        void throwsUnsupportedOperationException() {
            TipDTO dto = new TipDTO(
                    UUID.randomUUID(),
                    WarningStatus.GREEN,
                    ViolationType.OVER,
                    ViolatedSensor.TEMPERATURE,
                    "Open a window."
            );

            assertThrows(UnsupportedOperationException.class, () -> mapper.mapFrom(dto));
        }

        @Test
        @DisplayName("throws UnsupportedOperationException even when dto is null")
        void throwsEvenForNullDto() {
            assertThrows(UnsupportedOperationException.class, () -> mapper.mapFrom(null));
        }
    }
}
