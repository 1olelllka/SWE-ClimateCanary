package at.qe.skeleton.services.impl;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.ConfigRequestDTO;
import at.qe.skeleton.dtos.OccupancyDTO;
import at.qe.skeleton.dtos.PiConfigDTO;
import at.qe.skeleton.dtos.ReducedSensorDTO;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.mappers.LimitMapper;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.RoomOccupancy;
import at.qe.skeleton.model.RoomType;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomOccupancyRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.services.RaspberryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Implementation of {@link RaspberryService} providing CRUD and room-assignment
 * operations for {@link RaspberryPi} devices. Any change to a Pi's IP, port,
 * frequency, or room assignment triggers a {@link NotifyRaspberryCommand} event so
 * the device can re-fetch its configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaspberryServiceImpl implements RaspberryService {

    private final RaspberryPiRepository raspberryPiRepository;
    private final RoomMonitoringRepository monitoringRepository;
    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher;
    private final LimitMapper limitMapper;
    private final RoomOccupancyRepository occupancyRepository;
    private final RoomRepository roomRepository;

    /**
     * Returns a paginated list of all registered Raspberry Pi devices.
     *
     * @param pageable pagination parameters
     * @return page of {@link RaspberryPi} entities
     */
    @Override
    public Page<RaspberryPi> getAllRaspberries(Pageable pageable) {
        return raspberryPiRepository.findAll(pageable);
    }

    /**
     * Returns the Raspberry Pi with the given ID.
     *
     * @param id the device UUID
     * @return the matching {@link RaspberryPi}
     * @throws NotFoundException if no device with that ID exists
     */
    @Override
    public RaspberryPi getSpecificRaspberry(UUID id) {
        return raspberryPiRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi device with id %s was not found".formatted(id.toString())));
    }

    /**
     * Registers a new Raspberry Pi. Both the name and the IP:port combination must
     * be unique across all existing devices.
     *
     * @param raspberryPi the device to register
     * @return the saved {@link RaspberryPi}
     * @throws ConflictException if the name or IP:port is already taken
     */
    @Override
    public RaspberryPi createNewRaspberry(RaspberryPi raspberryPi) {
        if (raspberryPiRepository.existsByName(raspberryPi.getName())) {
            throw new ConflictException("Raspberry Pi with name " + raspberryPi.getName() + " already exists.");
        }
        if (raspberryPiRepository.existsByIpAndPort(raspberryPi.getIp(), raspberryPi.getPort())) {
            throw new ConflictException("Raspberry Pi with this ip and port already exists.");
        }
        log.info("Added new Raspberry Pi '{}' [{}:{}] into system .", raspberryPi.getName(), raspberryPi.getIp(), raspberryPi.getPort());
        return raspberryPiRepository.save(raspberryPi);
    }

    /**
     * Applies a partial update to an existing Raspberry Pi. Only non-null fields are
     * applied. If the IP, port, or frequency changes, a {@link NotifyRaspberryCommand}
     * is published so the device re-fetches its configuration.
     *
     * @param id          the UUID of the device to update
     * @param raspberryPi a partial {@link RaspberryPi} carrying the fields to update
     * @return the updated {@link RaspberryPi}
     * @throws NotFoundException if no device with that ID exists
     * @throws ConflictException if the new name or IP:port conflicts with another device
     */
    @Override
    public RaspberryPi updateRaspberryById(UUID id, RaspberryPi raspberryPi) {
        return raspberryPiRepository.findById(id).map(rasp -> {
            log.info("Updating Raspberry Pi configs: '{}' [{}:{}]", rasp.getName(), rasp.getIp(), rasp.getPort());
            AtomicBoolean configChanges = new AtomicBoolean(false);
            Optional.ofNullable(raspberryPi.getFrequency()).ifPresent(freq -> {
                log.info("Pre-saved new frequency for Raspberry Pi sensors: {} -> {}", rasp.getFrequency(), freq);
                rasp.setFrequency(freq);
                configChanges.set(true);
            });
            Optional.ofNullable(raspberryPi.getName()).ifPresent(name -> {
                if (!rasp.getName().equals(name) && raspberryPiRepository.existsByName(name)) {
                    log.info("Failed to update Raspberry Pi - conflicting names: {} -> {}", rasp.getName(), name);
                    throw new ConflictException("Raspberry Pi with this name already exists.");
                }
                log.info("Pre-saved new name for Raspberry Pi: {} -> {}", rasp.getName(), name);
                rasp.setName(name);
            });
            AtomicBoolean ipPortChanged = new AtomicBoolean(false);
            Optional.ofNullable(raspberryPi.getIp()).ifPresent(ip -> {
                if (!ip.equals(rasp.getIp())) {
                    ipPortChanged.set(true);
                    log.info("Pre-saved new IP-address for Raspberry Pi: {} -> {}", rasp.getIp(), ip);
                    rasp.setIp(ip);
                }
            });
            Optional.ofNullable(raspberryPi.getPort()).ifPresent(port -> {
                if (!port.equals(rasp.getPort())) {
                    ipPortChanged.set(true);
                    log.info("Pre-saved new port for Raspberry Pi: {} -> {}", rasp.getPort(), port);
                    rasp.setPort(port);
                }
            });
            if (ipPortChanged.get()) {
                if (raspberryPiRepository.existsByIpAndPort(rasp.getIp(), rasp.getPort())) {
                    log.info("Failed to update Raspberry Pi - conflicting ip:port: [{}:{}] -> [{}:{}]", rasp.getIp(), rasp.getPort(), rasp.getIp(), rasp.getPort());
                    throw new ConflictException("Raspberry Pi with this ip and port already exists.");
                }
            }
            log.info("Successfully updated Raspberry Pi configs: '{}' [{}:{}]", rasp.getName(), rasp.getIp(), rasp.getPort());
            RaspberryPi pi = raspberryPiRepository.save(rasp);
            if (ipPortChanged.get() || configChanges.get())
                eventPublisher.publishEvent(new NotifyRaspberryCommand(
                        new ConfigRequestDTO(pi.getId(), LocalDateTime.now()), pi, notificationClient
                ));
            return pi;
        }).orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
    }

    /**
     * Deletes the Raspberry Pi with the given ID. If the device is assigned to a room,
     * the room monitoring record is updated to remove the reference before deletion.
     * If no device with that ID exists, this method is a no-op.
     *
     * @param id the UUID of the device to delete
     */
    @Override
    @Transactional
    public void deleteRaspberry(UUID id) {
        Optional<RaspberryPi> optional = raspberryPiRepository.findById(id);
        if (optional.isPresent()) {
            RaspberryPi pi = optional.get();
            if (pi.getRoomMonitoring() != null) {
                log.info("Deleting Raspberry Pi '{}' from the room {}...", pi.getName(), pi.getRoomMonitoring().getRoomNumber());
                pi.getRoomMonitoring().setRaspberryPi(null);
                monitoringRepository.save(pi.getRoomMonitoring());
            }
            raspberryPiRepository.deleteById(id);
            log.info("Raspberry Pi '{}' deleted.", pi.getName());
        }
    }

    /**
     * Returns the current room occupancy stored in Redis for the room assigned to the
     * given Raspberry Pi. Returns {@code null} if the Pi has no assigned room.
     *
     * @param id the Raspberry Pi UUID
     * @return the {@link RoomOccupancy}, or {@code null} if no room is assigned
     * @throws NotFoundException if no device with that ID exists
     */
    @Override
    @Transactional
    public RoomOccupancy getOccupancyFromRedis(UUID id) {
        RaspberryPi pi = raspberryPiRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
        if (pi.getRoomMonitoring() == null) return null;
        return occupancyRepository.findById(pi.getRoomMonitoring().getRoomId().toString()).orElse(new RoomOccupancy(pi.getRoomMonitoring().getRoomId(), 0));
    }

    /**
     * Builds and returns the full configuration payload for the given Raspberry Pi,
     * including sensor UUIDs, room limits, occupancy, and measurement frequency.
     * If the Pi has no assigned room, a minimal config with {@code null} room fields
     * is returned.
     *
     * @param id the Raspberry Pi UUID
     * @return the {@link PiConfigDTO} for the device
     * @throws NotFoundException if no device with that ID exists
     */
    @Override
    @Transactional
    public PiConfigDTO getConfigForRaspberry(UUID id) {
        RaspberryPi pi = raspberryPiRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
        Set<ReducedSensorDTO> sensors = pi.getRoomMonitoring() != null && pi.getRoomMonitoring().getSensorStations() != null
                ? pi.getRoomMonitoring().getSensorStations().stream()
                .map(sensor -> new ReducedSensorDTO(sensor.getName(), sensor.getReadId(), sensor.getWriteId()))
                .collect(Collectors.toSet())
                : Set.of();
        if (sensors.isEmpty()) {
            log.info("There are no sensors connected to RaspberryPi {}", pi.getName());
        } else {
            log.info("Found {} sensors connected to RaspberryPi {}", sensors.size(), pi.getName());
        }
        RoomOccupancy occupancy = getOccupancyFromRedis(id);
        if (pi.getRoomMonitoring() != null) {
            UUID roomId = pi.getRoomMonitoring().getRoomId();
            boolean isSharedRoom = roomRepository.findById(roomId)
                    .map(room -> room.getRoomType() == RoomType.SHARED)
                    .orElse(false);
            return new PiConfigDTO(roomId,
                    id,
                    pi.getFrequency(),
                    limitMapper.mapTo(pi.getRoomMonitoring()),
                    sensors,
                    occupancy != null ?
                            new OccupancyDTO(occupancy.getPeopleCnt(), occupancy.getRoomId(), !isSharedRoom && occupancy.getPeopleCnt() < 5)
                            : null);
        } else {
            log.info("No room is connected to Raspberry Pi {}", pi.getName());
            return new PiConfigDTO(null, id, pi.getFrequency(), null, null, null);
        }
    }

    /**
     * Publishes a {@link NotifyRaspberryCommand} to trigger a configuration re-check
     * on the given Raspberry Pi. Used to recover connectivity after a failure.
     *
     * @param raspberryPi the UUID of the device to retry
     * @throws NotFoundException if no device with that ID exists
     */
    @Override
    @Transactional
    public void retryConnection(UUID raspberryPi) {
        RaspberryPi pi = raspberryPiRepository.findById(raspberryPi)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + raspberryPi + " was not found."));
        log.info("Retrying connection with Raspberry Pi '{}' [{}:{}]", pi.getName(), pi.getIp(), pi.getPort());
        eventPublisher.publishEvent(
                new NotifyRaspberryCommand(
                        new ConfigRequestDTO(pi.getId(), LocalDateTime.now()),
                        pi,
                        notificationClient));
    }

    /**
     * Assigns a room to the given Raspberry Pi. If the room already has a different
     * Pi assigned, that Pi is notified to clear its config. The Pi itself is then
     * notified of the new room assignment.
     *
     * @param raspberryId the UUID of the Raspberry Pi
     * @param roomId      the UUID of the room to assign
     * @return the updated {@link RaspberryPi}
     * @throws NotFoundException  if the Pi or the room does not exist
     * @throws ConflictException  if the Pi already has a room assigned
     */
    @Override
    @Transactional
    public RaspberryPi addNewRoom(UUID raspberryId, UUID roomId) {
        RaspberryPi pi = raspberryPiRepository.findById(raspberryId)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + raspberryId + " was not found."));
        RoomMonitoring monitoring = monitoringRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
        if (pi.getRoomMonitoring() != null) {
            throw new ConflictException("Raspberry Pi already has a room assigned. Remove the current room first.");
        }
        pi.setRoomMonitoring(monitoring);
        if (monitoring.getRaspberryPi() != null) {
            log.info("Overriding the room for old raspberry '{}'... Updating config for old Raspberry...", pi.getName());
            eventPublisher.publishEvent(
                    new NotifyRaspberryCommand(new ConfigRequestDTO(monitoring.getRaspberryPi().getId(),
                            LocalDateTime.now()), monitoring.getRaspberryPi(), notificationClient)
            );
        }
        monitoring.setRaspberryPi(pi);
        monitoringRepository.save(monitoring);
        log.info("Added new room '{}' for Raspberry Pi '{}' in system.", monitoring.getRoomNumber(), pi.getName());
        log.info("Notifying Raspberry Pi '{}' about new room in config.", pi.getName());
        eventPublisher.publishEvent(
                new NotifyRaspberryCommand(
                        new ConfigRequestDTO(pi.getId(), LocalDateTime.now()),
                        pi,
                        notificationClient));
        return raspberryPiRepository.save(pi);
    }

    /**
     * Removes the assignment between the given Raspberry Pi and room. Both the Pi and
     * the room monitoring record are updated, and the Pi is notified to clear its
     * room configuration.
     *
     * @param raspberryId the UUID of the Raspberry Pi
     * @param roomId      the UUID of the room to remove
     * @return the updated {@link RaspberryPi}
     * @throws NotFoundException if the Pi or room does not exist, or the room is not
     *                           currently assigned to the given Pi
     */
    @Override
    @Transactional
    public RaspberryPi removeRoomFromRaspberry(UUID raspberryId, UUID roomId) {
        RaspberryPi pi = raspberryPiRepository.findById(raspberryId)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + raspberryId + " was not found."));
        RoomMonitoring monitoring = monitoringRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
        if (pi.getRoomMonitoring() == null || !pi.getRoomMonitoring().equals(monitoring)) {
            throw new NotFoundException("Cannot find this room inside of raspberry pi with id " + raspberryId);
        }
        pi.setRoomMonitoring(null);
        monitoring.setRaspberryPi(null);
        monitoringRepository.save(monitoring);
        log.info("Removed room '{}' from Raspberry Pi '{}'.", monitoring.getRoomNumber(), pi.getName());
        log.info("Notifying Raspberry Pi '{}' about room removal in config.", pi.getName());
        eventPublisher.publishEvent(
                new NotifyRaspberryCommand(
                        new ConfigRequestDTO(pi.getId(), LocalDateTime.now()),
                        pi,
                        notificationClient));
        return raspberryPiRepository.save(pi);
    }
}