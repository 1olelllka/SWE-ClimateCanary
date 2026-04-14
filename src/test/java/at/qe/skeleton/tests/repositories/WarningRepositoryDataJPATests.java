package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.model.MeasurementType;
import at.qe.skeleton.model.Warnings;
import at.qe.skeleton.repositories.WarningRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("WarningRepository")
class WarningRepositoryDataJPATests {

    @Autowired TestEntityManager em;
    @Autowired WarningRepository repository;

    private RoomMonitoring roomA;
    private RoomMonitoring roomB;
    private final LocalDateTime now = LocalDateTime.of(2024, 6, 1, 12, 0);

    @BeforeEach
    void setUp() {
        roomA = em.persist(RoomMonitoring.builder()
                .roomId(UUID.randomUUID()).roomNumber("A101").build());
        roomB = em.persist(RoomMonitoring.builder()
                .roomId(UUID.randomUUID()).roomNumber("B202").build());
    }

    @Nested
    @DisplayName("findByRoomMonitoring_RoomIdAndResolvedAtIsNull")
    class FindActiveForRoom {

        @Test
        @DisplayName("returns only active (unresolved) warnings for the room")
        void returnsOnlyActive() {
            persistWarning(roomA, null);           // active
            persistWarning(roomA, now.minusHours(1)); // resolved
            em.flush();

            List<Warnings> result = repository
                    .findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomA.getRoomId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getResolvedAt()).isNull();
        }

        @Test
        @DisplayName("returns empty list when all warnings are resolved")
        void returnsEmptyWhenAllResolved() {
            persistWarning(roomA, now.minusHours(2));
            persistWarning(roomA, now.minusHours(1));
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomA.getRoomId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("does not return active warnings from other rooms")
        void ignoresOtherRooms() {
            persistWarning(roomB, null); // active but wrong room
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomA.getRoomId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns all active warnings when multiple exist")
        void returnsMultipleActive() {
            persistWarning(roomA, null);
            persistWarning(roomA, null);
            persistWarning(roomA, now.minusHours(1)); // resolved — excluded
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomA.getRoomId()))
                    .hasSize(2);
        }
    }

    @Nested
    @DisplayName("findAllActive")
    class FindAllActive {

        @Test
        @DisplayName("returns active warnings across all rooms")
        void returnsActiveAcrossRooms() {
            persistWarning(roomA, null);              // active
            persistWarning(roomB, null);              // active
            persistWarning(roomA, now.minusHours(1)); // resolved
            em.flush();

            List<Warnings> result = repository.findAllActive();

            assertThat(result).hasSize(2)
                    .allMatch(w -> w.getResolvedAt() == null);
        }

        @Test
        @DisplayName("returns empty list when no active warnings exist")
        void returnsEmptyWhenNoneActive() {
            persistWarning(roomA, now.minusHours(2));
            persistWarning(roomB, now.minusHours(1));
            em.flush();

            assertThat(repository.findAllActive()).isEmpty();
        }

        @Test
        @DisplayName("returns empty list when no warnings exist at all")
        void returnsEmptyOnCleanDatabase() {
            assertThat(repository.findAllActive()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByRoomMonitoring_RoomId")
    class FindAllForRoom {

        @Test
        @DisplayName("returns both active and resolved warnings for the room")
        void returnsMixed() {
            persistWarning(roomA, null);
            persistWarning(roomA, now.minusHours(1));
            em.flush();

            List<Warnings> result = repository.findByRoomMonitoring_RoomId(roomA.getRoomId());

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("does not return warnings from other rooms")
        void ignoresOtherRooms() {
            persistWarning(roomB, null);
            persistWarning(roomB, now.minusHours(1));
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomId(roomA.getRoomId())).isEmpty();
        }

        @Test
        @DisplayName("returns empty list for room with no warnings")
        void returnsEmptyForCleanRoom() {
            assertThat(repository.findByRoomMonitoring_RoomId(roomA.getRoomId())).isEmpty();
        }
    }

    private void persistWarning(RoomMonitoring room, LocalDateTime resolvedAt) {
        em.persist(Warnings.builder()
                .message("Test warning")
                .triggeredValue(25.0)
                .activeLimitAtTime(20.0)
                .createdAt(now)
                .resolvedAt(resolvedAt)
                .status(resolvedAt == null ? WarningStatus.RED : WarningStatus.GREEN)
                .measurementType(MeasurementType.TEMPERATURE)
                .roomMonitoring(room)
                .build());
    }
}
