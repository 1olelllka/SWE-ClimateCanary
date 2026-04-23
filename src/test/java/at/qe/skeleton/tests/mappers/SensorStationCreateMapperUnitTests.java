//package at.qe.skeleton.tests.mappers;
//
//import at.qe.skeleton.dtos.SensorStationCreateDTO;
//import at.qe.skeleton.exceptions.NotFoundException;
//import at.qe.skeleton.mappers.SensorStationCreateMapper;
//import at.qe.skeleton.model.DeviceStatus;
//import at.qe.skeleton.model.RoomMonitoring;
//import at.qe.skeleton.model.SensorStation;
//import at.qe.skeleton.repositories.RoomMonitoringRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//@DisplayName("SensorStationCreateMapper")
//class SensorStationCreateMapperUnitTests {
//
//    private RoomMonitoringRepository repository;
//    private SensorStationCreateMapper mapper;
//
//    @BeforeEach
//    void setUp() {
//        repository = mock(RoomMonitoringRepository.class);
//        mapper = new SensorStationCreateMapper(repository);
//    }
//
//    private SensorStationCreateDTO buildDto(String name, UUID roomId) {
//        return new SensorStationCreateDTO(name, roomId);
//    }
//
//    @Nested
//    @DisplayName("mapTo(SensorStation) — unsupported direction")
//    class MapTo {
//
//        @Test
//        @DisplayName("throws UnsupportedOperationException — create DTO is write-only")
//        void throwsUnsupportedOperationException() {
//            assertThatThrownBy(() -> mapper.mapTo(new SensorStation()))
//                    .isInstanceOf(UnsupportedOperationException.class);
//        }
//
//        @Test
//        @DisplayName("throws UnsupportedOperationException even when entity is null")
//        void throwsEvenForNullEntity() {
//            assertThrows(UnsupportedOperationException.class, () -> mapper.mapTo(null));
//        }
//    }
//
//    @Nested
//    @DisplayName("mapFrom(SensorStationCreateDTO) — DTO to Entity")
//    class MapFrom {
//
//        private UUID roomId;
//        private RoomMonitoring room;
//
//        @BeforeEach
//        void setUp() {
//            roomId = UUID.randomUUID();
//            room = new RoomMonitoring();
//            room.setRoomId(roomId);
//            when(repository.findById(roomId)).thenReturn(Optional.of(room));
//        }
//
//        @Test
//        @DisplayName("maps name correctly")
//        void mapsName() {
//            SensorStation result = mapper.mapFrom(buildDto("Station Alpha", roomId));
//
//            assertThat(result.getName()).isEqualTo("Station Alpha");
//        }
//
//        @Test
//        @DisplayName("sets initial status to OFFLINE")
//        void setsStatusToOffline() {
//            SensorStation result = mapper.mapFrom(buildDto("Station Alpha", roomId));
//
//            assertThat(result.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
//        }
//
//        @Test
//        @DisplayName("sets lastHeartBeat to null — station has never connected")
//        void setsLastHeartBeatToNull() {
//            SensorStation result = mapper.mapFrom(buildDto("Station Alpha", roomId));
//
//            assertThat(result.getLastHeartBeat()).isNull();
//        }
//
//        @Test
//        @DisplayName("does not set id — left for JPA generation")
//        void doesNotSetId() {
//            SensorStation result = mapper.mapFrom(buildDto("Station Alpha", roomId));
//
//            assertThat(result.getId()).isNull();
//        }
//
//        @Test
//        @DisplayName("resolves roomMonitoring from repository via roomId")
//        void resolvesRoomMonitoring() {
//            SensorStation result = mapper.mapFrom(buildDto("Station Alpha", roomId));
//
//            assertThat(result.getRoomMonitoring()).isEqualTo(room);
//        }
//
//        @Test
//        @DisplayName("throws NotFoundException when roomId does not exist")
//        void throwsNotFoundWhenRoomMissing() {
//            UUID unknownId = UUID.randomUUID();
//            when(repository.findById(unknownId)).thenReturn(Optional.empty());
//
//            assertThatThrownBy(() -> mapper.mapFrom(buildDto("Station Alpha", unknownId)))
//                    .isInstanceOf(NotFoundException.class)
//                    .hasMessageContaining(unknownId.toString());
//        }
//
//        @Test
//        @DisplayName("throws NullPointerException when dto is null")
//        void throwsOnNullDto() {
//            assertThrows(NullPointerException.class, () -> mapper.mapFrom(null));
//        }
//    }
//}