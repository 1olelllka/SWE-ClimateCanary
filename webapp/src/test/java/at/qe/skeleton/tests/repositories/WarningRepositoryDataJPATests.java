package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.MeasurementType;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.model.Warnings;
import at.qe.skeleton.repositories.WarningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

    private final LocalDateTime now = LocalDateTime.of(2024, 6, 15, 12, 0);

    @BeforeEach
    void setUp() {
        roomA = em.persist(RoomMonitoring.builder()
                .roomId(UUID.randomUUID()).roomNumber("A101").build());
        roomB = em.persist(RoomMonitoring.builder()
                .roomId(UUID.randomUUID()).roomNumber("B202").build());
    }

    // ── findByRoomMonitoring_RoomIdAndResolvedAtIsNull ───────────────────────────

    @Nested
    @DisplayName("findByRoomMonitoring_RoomIdAndResolvedAtIsNull")
    class FindActiveForRoom {

        @Test
        @DisplayName("returns only active (unresolved) warnings for the room")
        void returnsOnlyActive() {
            persist(roomA, now, null);
            persist(roomA, now, now.minusHours(1));
            em.flush();

            List<Warnings> result = repository
                    .findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomA.getRoomId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getResolvedAt()).isNull();
        }

        @Test
        @DisplayName("returns empty list when all warnings are resolved")
        void returnsEmptyWhenAllResolved() {
            persist(roomA, now, now.minusHours(2));
            persist(roomA, now, now.minusHours(1));
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomA.getRoomId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("does not return active warnings from other rooms")
        void ignoresOtherRooms() {
            persist(roomB, now, null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomA.getRoomId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns all active warnings when multiple exist for the same room")
        void returnsMultipleActive() {
            persist(roomA, now, null);
            persist(roomA, now, null);
            persist(roomA, now, now.minusHours(1));
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomA.getRoomId()))
                    .hasSize(2);
        }
    }

    // ── findAllActive ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAllActive")
    class FindAllActive {

        @Test
        @DisplayName("returns active warnings across all rooms")
        void returnsActiveAcrossRooms() {
            persist(roomA, now, null);
            persist(roomB, now, null);
            persist(roomA, now, now.minusHours(1));
            em.flush();

            List<Warnings> result = repository.findAllActive();

            assertThat(result).hasSize(2)
                    .allMatch(w -> w.getResolvedAt() == null);
        }

        @Test
        @DisplayName("returns empty list when no active warnings exist")
        void returnsEmptyWhenNoneActive() {
            persist(roomA, now, now.minusHours(2));
            persist(roomB, now, now.minusHours(1));
            em.flush();

            assertThat(repository.findAllActive()).isEmpty();
        }

        @Test
        @DisplayName("returns empty list when repository is empty")
        void returnsEmptyOnCleanDatabase() {
            assertThat(repository.findAllActive()).isEmpty();
        }
    }

    // ── findByRoomMonitoring_RoomId ──────────────────────────────────────────────

    @Nested
    @DisplayName("findByRoomMonitoring_RoomId")
    class FindAllForRoom {

        @Test
        @DisplayName("returns both active and resolved warnings for the room")
        void returnsMixed() {
            persist(roomA, now, null);
            persist(roomA, now, now.minusHours(1));
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomId(roomA.getRoomId())).hasSize(2);
        }

        @Test
        @DisplayName("does not return warnings from other rooms")
        void ignoresOtherRooms() {
            persist(roomB, now, null);
            persist(roomB, now, now.minusHours(1));
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomId(roomA.getRoomId())).isEmpty();
        }

        @Test
        @DisplayName("returns empty list for room with no warnings")
        void returnsEmptyForCleanRoom() {
            assertThat(repository.findByRoomMonitoring_RoomId(roomA.getRoomId())).isEmpty();
        }
    }

    // ── findByRoomMonitoring_RoomIdAndCreatedAtBetween ───────────────────────────

    @Nested
    @DisplayName("findByRoomMonitoring_RoomIdAndCreatedAtBetween")
    class FindByRoomAndDateRange {

        private final LocalDateTime rangeStart = now.minusDays(1);
        private final LocalDateTime rangeEnd   = now.plusDays(1);

        @Test
        @DisplayName("returns warning whose createdAt falls within range")
        void returnsWithinRange() {
            persist(roomA, now, null); // createdAt = now, inside [now-1d, now+1d]
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                    roomA.getRoomId(), rangeStart, rangeEnd))
                    .hasSize(1);
        }

        @Test
        @DisplayName("excludes warning whose createdAt is before range start")
        void excludesBeforeRange() {
            persist(roomA, now.minusDays(2), null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                    roomA.getRoomId(), rangeStart, rangeEnd))
                    .isEmpty();
        }

        @Test
        @DisplayName("excludes warning whose createdAt is after range end")
        void excludesAfterRange() {
            persist(roomA, now.plusDays(2), null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                    roomA.getRoomId(), rangeStart, rangeEnd))
                    .isEmpty();
        }

        @Test
        @DisplayName("includes warning whose createdAt is exactly on range start boundary")
        void includesOnStartBoundary() {
            persist(roomA, rangeStart, null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                    roomA.getRoomId(), rangeStart, rangeEnd))
                    .hasSize(1);
        }

        @Test
        @DisplayName("includes warning whose createdAt is exactly on range end boundary")
        void includesOnEndBoundary() {
            persist(roomA, rangeEnd, null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                    roomA.getRoomId(), rangeStart, rangeEnd))
                    .hasSize(1);
        }

        @Test
        @DisplayName("does not return warnings from other rooms even if in range")
        void ignoresOtherRooms() {
            persist(roomB, now, null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                    roomA.getRoomId(), rangeStart, rangeEnd))
                    .isEmpty();
        }
    }

    // ── findByRoomMonitoring_RoomIdInAndResolvedAtIsNull ─────────────────────────

    @Nested
    @DisplayName("findByRoomMonitoring_RoomIdInAndResolvedAtIsNull")
    class FindActiveByRoomList {

        @Test
        @DisplayName("returns active warnings for multiple rooms")
        void returnsActiveForMultipleRooms() {
            persist(roomA, now, null);
            persist(roomB, now, null);
            persist(roomB, now, now.minusHours(1));
            em.flush();

            List<Warnings> result = repository
                    .findByRoomMonitoring_RoomIdInAndResolvedAtIsNull(
                            List.of(roomA.getRoomId(), roomB.getRoomId()));

            assertThat(result).hasSize(2)
                    .allMatch(w -> w.getResolvedAt() == null);
        }

        @Test
        @DisplayName("excludes resolved warnings")
        void excludesResolved() {
            persist(roomA, now, now.minusHours(1));
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNull(
                    List.of(roomA.getRoomId())))
                    .isEmpty();
        }

        @Test
        @DisplayName("does not return warnings from rooms not in the list")
        void ignoresRoomsNotInList() {
            persist(roomB, now, null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNull(
                    List.of(roomA.getRoomId())))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns empty list when passed an empty room list")
        void returnsEmptyForEmptyRoomList() {
            persist(roomA, now, null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNull(List.of()))
                    .isEmpty();
        }
    }

    // ── findByRoomMonitoring_RoomIdInAndCreatedAtBetween ─────────────────────────

    @Nested
    @DisplayName("findByRoomMonitoring_RoomIdInAndCreatedAtBetween")
    class FindByRoomsAndDateRange {

        private final LocalDateTime rangeStart = now.minusDays(1);
        private final LocalDateTime rangeEnd   = now.plusDays(1);

        @Test
        @DisplayName("returns warnings across multiple rooms within range")
        void returnsAcrossRoomsInRange() {
            persist(roomA, now, null);
            persist(roomB, now, null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdInAndCreatedAtBetween(
                    List.of(roomA.getRoomId(), roomB.getRoomId()), rangeStart, rangeEnd))
                    .hasSize(2);
        }

        @Test
        @DisplayName("excludes warnings outside date range")
        void excludesOutOfRange() {
            persist(roomA, now.minusDays(3), null);
            persist(roomB, now.plusDays(3),  null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdInAndCreatedAtBetween(
                    List.of(roomA.getRoomId(), roomB.getRoomId()), rangeStart, rangeEnd))
                    .isEmpty();
        }

        @Test
        @DisplayName("does not return warnings from rooms not in the list")
        void ignoresRoomsNotInList() {
            persist(roomB, now, null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdInAndCreatedAtBetween(
                    List.of(roomA.getRoomId()), rangeStart, rangeEnd))
                    .isEmpty();
        }
    }

    // ── findByRoomMonitoring_RoomIdInAndResolvedAtIsNullAndCreatedAtBetween ──────

    @Nested
    @DisplayName("findByRoomMonitoring_RoomIdInAndResolvedAtIsNullAndCreatedAtBetween")
    class FindActiveByRoomsAndDateRange {

        private final LocalDateTime rangeStart = now.minusDays(1);
        private final LocalDateTime rangeEnd   = now.plusDays(1);

        @Test
        @DisplayName("returns only active warnings within range across multiple rooms")
        void returnsActiveInRange() {
            persist(roomA, now, null);
            persist(roomB, now, now.minusHours(1));
            em.flush();

            List<Warnings> result = repository
                    .findByRoomMonitoring_RoomIdInAndResolvedAtIsNullAndCreatedAtBetween(
                            List.of(roomA.getRoomId(), roomB.getRoomId()), rangeStart, rangeEnd);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getResolvedAt()).isNull();
        }

        @Test
        @DisplayName("excludes resolved warnings even if within range")
        void excludesResolvedEvenIfInRange() {
            persist(roomA, now, now.minusHours(1));
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNullAndCreatedAtBetween(
                    List.of(roomA.getRoomId()), rangeStart, rangeEnd))
                    .isEmpty();
        }

        @Test
        @DisplayName("excludes active warnings outside date range")
        void excludesActiveOutOfRange() {
            persist(roomA, now.minusDays(3), null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNullAndCreatedAtBetween(
                    List.of(roomA.getRoomId()), rangeStart, rangeEnd))
                    .isEmpty();
        }

        @Test
        @DisplayName("does not return warnings from rooms not in the list")
        void ignoresRoomsNotInList() {
            persist(roomB, now, null);
            em.flush();

            assertThat(repository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNullAndCreatedAtBetween(
                    List.of(roomA.getRoomId()), rangeStart, rangeEnd))
                    .isEmpty();
        }
    }

    // ── helper ───────────────────────────────────────────────────────────────────

    private void persist(RoomMonitoring room, LocalDateTime createdAt, LocalDateTime resolvedAt) {
        em.persist(Warnings.builder()
                .message("Test warning")
                .deviceName("Test Device")
                .triggeredValue(25.0)
                .activeLimitAtTime(20.0)
                .createdAt(createdAt)
                .resolvedAt(resolvedAt)
                .status(resolvedAt == null ? WarningStatus.RED : WarningStatus.GREEN)
                .measurementType(MeasurementType.TEMPERATURE)
                .roomMonitoring(room)
                .build());
    }
}