package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.TipCreateDTO;
import at.qe.skeleton.mappers.TipCreateMapper;
import at.qe.skeleton.model.Tip;
import at.qe.skeleton.model.ViolatedSensor;
import at.qe.skeleton.model.ViolationType;
import at.qe.skeleton.model.WarningStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("TipCreateMapper")
class TipCreateMapperUnitTests {

    private TipCreateMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TipCreateMapper();
    }


    @Nested
    @DisplayName("mapTo(Tip) — unsupported direction")
    class MapTo {

        @Test
        @DisplayName("throws UnsupportedOperationException — create DTO is write-only")
        void throwsUnsupportedOperationException() {
            assertThatThrownBy(() -> mapper.mapTo(new Tip()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("throws UnsupportedOperationException even when entity is null")
        void throwsEvenForNullEntity() {
            assertThrows(UnsupportedOperationException.class,
                    () -> mapper.mapTo(null));
        }
    }


    @Nested
    @DisplayName("mapFrom(TipCreateDTO) — DTO to Entity")
    class MapFrom {

        @Test
        @DisplayName("maps all fields correctly")
        void mapsAllFields() {
            TipCreateDTO dto = new TipCreateDTO(
                    ViolationType.OVER,
                    ViolatedSensor.TEMPERATURE,
                    WarningStatus.RED,
                    "Open a window to reduce temperature."
            );

            Tip result = mapper.mapFrom(dto);

            assertThat(result).isNotNull();
            assertThat(result.getMsg()).isEqualTo("Open a window to reduce temperature.");
            assertThat(result.getViolationType()).isEqualTo(ViolationType.OVER);
            assertThat(result.getViolatedSensor()).isEqualTo(ViolatedSensor.TEMPERATURE);
            assertThat(result.getViolationStatus()).isEqualTo(WarningStatus.RED);
        }

        @Test
        @DisplayName("maps HUMIDITY violated sensor correctly")
        void mapsHumiditySensor() {
            TipCreateDTO dto = new TipCreateDTO(
                    ViolationType.OVER,
                    ViolatedSensor.HUMIDITY,
                    WarningStatus.GREEN,
                    "Use a dehumidifier."
            );

            Tip result = mapper.mapFrom(dto);

            assertThat(result.getViolatedSensor()).isEqualTo(ViolatedSensor.HUMIDITY);
            assertThat(result.getViolationStatus()).isEqualTo(WarningStatus.GREEN);
        }

        @Test
        @DisplayName("maps CO2 violated sensor correctly")
        void mapsCo2Sensor() {
            TipCreateDTO dto = new TipCreateDTO(
                    ViolationType.OVER,
                    ViolatedSensor.AIR,
                    WarningStatus.GREEN,
                    "Ventilate the room immediately."
            );

            Tip result = mapper.mapFrom(dto);

            assertThat(result.getViolatedSensor()).isEqualTo(ViolatedSensor.AIR);
            assertThat(result.getViolationStatus()).isEqualTo(WarningStatus.GREEN);
        }

        @Test
        @DisplayName("maps all ViolationType values correctly")
        void mapsAllViolationTypes() {
            for (ViolationType type : ViolationType.values()) {
                TipCreateDTO dto = new TipCreateDTO(
                        type,
                        ViolatedSensor.TEMPERATURE,
                        WarningStatus.GREEN,
                        "Some tip message."
                );

                Tip result = mapper.mapFrom(dto);

                assertThat(result.getViolationType()).isEqualTo(type);
            }
        }

        @Test
        @DisplayName("maps all ViolatedSensor values correctly")
        void mapsAllViolatedSensors() {
            for (ViolatedSensor sensor : ViolatedSensor.values()) {
                TipCreateDTO dto = new TipCreateDTO(
                        ViolationType.OVER,
                        sensor,
                        WarningStatus.GREEN,
                        "Some tip message."
                );

                Tip result = mapper.mapFrom(dto);

                assertThat(result.getViolatedSensor()).isEqualTo(sensor);
            }
        }

        @Test
        @DisplayName("does not set id — left for JPA generation")
        void doesNotSetId() {
            TipCreateDTO dto = new TipCreateDTO(
                    ViolationType.OVER,
                    ViolatedSensor.TEMPERATURE,
                    WarningStatus.GREEN,
                    "Open a window."
            );

            Tip result = mapper.mapFrom(dto);

            assertThat(result.getId()).isNull();
        }


        @Test
        @DisplayName("throws NullPointerException when dto is null")
        void throwsOnNullDto() {
            assertThrows(NullPointerException.class, () -> mapper.mapFrom(null));
        }
    }
}