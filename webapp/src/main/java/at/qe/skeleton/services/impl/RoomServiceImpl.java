package at.qe.skeleton.services.impl;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.LimitChangeNotificationDTO;
import at.qe.skeleton.dtos.LimitDTO;
import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of {@link RoomService} providing CRUD operations for {@link Room}
 * entities and their associated {@link RoomMonitoring} configuration. Creating a room
 * automatically provisions a {@link RoomMonitoring} record with default (empty) limits.
 * Deleting a room clears all user assignments, detaches the room's Raspberry Pi (notifying
 * it with a {@link UpdateType#FLUSH} command), and removes all aggregated stats.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final AggregatedStatsRepository aggregatedStatsRepository;
    private final RoomMonitoringRepository monitoringRepository;
    private final UserxRepository userxRepository;
    private final RaspberryPiRepository raspberryPiRepository;
    private final DepartmentRepository departmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationClient notificationClient;

    /**
     * Returns a paginated list of all rooms.
     *
     * @param pageable pagination parameters
     * @return page of {@link Room} entities
     */
    public Page<Room> getPageOfRooms(Pageable pageable) {
        return roomRepository.findAll(pageable);
    }

    /**
     * Creates a new room and its corresponding {@link RoomMonitoring} record with
     * default temperature, humidity, and CO₂ limits.
     *
     * @param room the room to create; must reference an existing department
     * @return the saved {@link Room}
     * @throws NotFoundException if the referenced department does not exist
     * @throws ConflictException if a room with the same number already exists in that department
     */
    @Transactional
    public Room createRoom(Room room) {
        if (!departmentRepository.existsById(room.getDepartment().getId()))
            throw new NotFoundException("Department with id %s was not found".formatted(room.getDepartment().getId()));
        if (roomRepository.existsByRoomNumberAndDepartmentId(room.getRoomNumber(), room.getDepartment().getId())) {
            throw new ConflictException("Room with this name already exists in this department.");
        }
        Room r = roomRepository.save(room);
        RoomMonitoring monitoring = RoomMonitoring.builder()
                .roomId(r.getId())
                .roomNumber(r.getRoomNumber())
                .humLimit(HumidityLimit.builder().build())
                .tempLimit(TemperatureLimit.builder().build())
                .polLimit(PollutionLimit.builder().build())
                .build();
        log.info("New room created {}", room.getRoomNumber());
        monitoringRepository.save(monitoring);
        return r;
    }

    /**
     * Applies a partial update to the room with the given ID. Only non-null fields are
     * applied. If the room number changes, the associated {@link RoomMonitoring} record
     * is updated to stay in sync. If users are provided, they are re-assigned to this
     * room.
     *
     * @param id   the UUID of the room to update
     * @param room a partial {@link Room} carrying the fields to update
     * @return the updated {@link Room}
     * @throws NotFoundException if the room or any referenced user does not exist
     * @throws ConflictException if the new room number already exists in the department
     */
    @Override
    @Transactional
    public Room patchRoom(UUID id, Room room) {
        return roomRepository.findById(id).map(r -> {
            Optional.ofNullable(room.getRoomType()).ifPresent(r::setRoomType);
            Optional.ofNullable(room.getDepartment()).ifPresent(r::setDepartment);
            Optional.ofNullable(room.getIsActive()).ifPresent(r::setIsActive);
            Optional.ofNullable(room.getDefaultPeopleCnt()).ifPresent(r::setDefaultPeopleCnt);
            Optional.ofNullable(room.getRoomNumber()).ifPresent(number -> {
                UUID deptId = Optional.ofNullable(room.getDepartment()).map(d -> d.getId())
                        .orElse(r.getDepartment().getId());
                if (roomRepository.existsByRoomNumberAndDepartmentId(number, deptId)) {
                    throw new ConflictException("Room with this name already exists in this department.");
                }
                RoomMonitoring m = monitoringRepository.findById(r.getId()).get(); // it should exist
                m.setRoomNumber(number);
                monitoringRepository.save(m);
                r.setRoomNumber(number);
            });
            if (room.getUsers() != null) {
                Set<Userx> foundUser = new HashSet<>();
                for (Userx user : room.getUsers()) {
                    Userx u = userxRepository.findById(user.getId()).orElseThrow(() -> new NotFoundException("User with id %s was not found.".formatted(user.getId())));
                    u.setMyRoom(r);
                    foundUser.add(u);
                }
                r.setUsers(foundUser);
            }
            return roomRepository.save(r);
        }).orElseThrow(() -> new NotFoundException("Room not found with id: " + id));
    }

    /**
     * Returns the {@link RoomMonitoring} record for the given room.
     *
     * @param roomId the room UUID
     * @return the room's monitoring configuration
     * @throws NotFoundException if no monitoring record exists for that room
     */
    @Override
    public RoomMonitoring getRoomMonitoring(UUID roomId) {
        return monitoringRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
    }

    /**
     * Updates the sensor limits for the given room. Only non-null fields in the DTO are
     * applied. Min/max consistency is validated for temperature and humidity. If a
     * Raspberry Pi is assigned to the room, it is notified of the new limits.
     *
     * @param roomId the room UUID
     * @param dto    the limit values to apply (null fields are ignored)
     * @return the updated {@link RoomMonitoring}
     * @throws NotFoundException   if no monitoring record exists for that room
     * @throws ValidationException if a minimum limit exceeds its corresponding maximum
     */
    @Override
    @Transactional
    public RoomMonitoring updateLimits(UUID roomId, LimitDTO dto) {
        return monitoringRepository.findById(roomId).map(room -> {
            Optional.ofNullable(dto.tempMax()).ifPresent(room.getTempLimit()::setMaxVal);
            Optional.ofNullable(dto.tempMin()).ifPresent(room.getTempLimit()::setMinVal);
            Optional.ofNullable(dto.co2Max()).ifPresent(room.getPolLimit()::setMaxVal);
            Optional.ofNullable(dto.humMax()).ifPresent(room.getHumLimit()::setMaxVal);
            Optional.ofNullable(dto.humMin()).ifPresent(room.getHumLimit()::setMinVal);
            if (room.getTempLimit().getMinVal() != null && room.getTempLimit().getMinVal() > room.getTempLimit().getMaxVal())
                throw new ValidationException("Minimal temperature cannot be greater than maximum.");
            if (room.getHumLimit().getMinVal() != null && room.getHumLimit().getMinVal() > room.getHumLimit().getMaxVal())
                throw new ValidationException("Minimal humidity cannot be greater than maximum.");
            RoomMonitoring r =  monitoringRepository.save(room);
            if (r.getRaspberryPi() != null) {
                eventPublisher.publishEvent(new NotifyRaspberryCommand(
                        new LimitChangeNotificationDTO(r.getTempLimit().getMinVal(), r.getTempLimit().getMaxVal(),
                                                        r.getHumLimit().getMinVal(), r.getHumLimit().getMaxVal(),
                                                        r.getPolLimit().getMaxVal(), LocalDateTime.now()),
                        r.getRaspberryPi(), notificationClient
                ));
            }
            log.info("Limits for the room {} updated successfully", room.getRoomId());
            return r;
        }).orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
    }

    /**
     * Deletes the room with the given ID. Performs the following cleanup in order:
     * <ol>
     *   <li>Clears the room assignment for all users in the room.</li>
     *   <li>Removes the room from its department's room list.</li>
     *   <li>If a Raspberry Pi is assigned, clears the Pi's room reference and sends
     *       a {@link UpdateType#FLUSH} command to reset the device.</li>
     *   <li>Deletes the {@link RoomMonitoring} record and all aggregated stats.</li>
     * </ol>
     * If no room with that ID exists, the monitoring record and stats are still cleaned up.
     *
     * @param id the UUID of the room to delete
     */
    @Override
    @Transactional
    public void deleteRoom(UUID id) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room != null) {
            room.getUsers().forEach(user -> {
                user.setMyRoom(null);
                userxRepository.save(user);
            });
            room.getDepartment().getRooms().remove(room);
            room.setDepartment(null);
        }
        if (room != null) roomRepository.delete(room);
        RoomMonitoring monitoring = monitoringRepository.findById(id).orElse(null);
        if (monitoring != null && monitoring.getRaspberryPi() != null) {
            monitoring.getRaspberryPi().setRoomMonitoring(null);
            raspberryPiRepository.save(monitoring.getRaspberryPi());
            eventPublisher.publishEvent(
                    new NotifyRaspberryCommand(
                            new StateChangeNotificationDTO(UpdateType.FLUSH, LocalDateTime.now()),
                            null,
                            null,
                            monitoring.getRaspberryPi(),
                            notificationClient)
            );
        }
        log.info("Deleted room {} - {}", id, room != null ? room.getRoomNumber() : "{UNKNOWN}");
        monitoringRepository.deleteById(id);
        aggregatedStatsRepository.deleteAllByRoomId(id);
    }
}