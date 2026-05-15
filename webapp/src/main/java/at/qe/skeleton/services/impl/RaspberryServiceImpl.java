package at.qe.skeleton.services.impl;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.*;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.mappers.LimitMapper;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.RoomOccupancy;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomOccupancyRepository;
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

    @Override
    public Page<RaspberryPi> getAllRaspberries(Pageable pageable) {
        return raspberryPiRepository.findAll(pageable);
    }

    @Override
    public RaspberryPi getSpecificRaspberry(UUID id) {
        return raspberryPiRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi device with id %s was not found".formatted(id.toString())));
    }

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

    @Override
    @Transactional
    public RoomOccupancy getOccupancyFromRedis(UUID id) {
        RaspberryPi pi = raspberryPiRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
        if (pi.getRoomMonitoring() == null) return null;
        return occupancyRepository.findById(pi.getRoomMonitoring().getRoomId().toString()).orElse(new RoomOccupancy(pi.getRoomMonitoring().getRoomId(), 0));
    }

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
        RoomOccupancy occupancy = getOccupancyFromRedis(id);
        if (pi.getRoomMonitoring() != null) {
            return new PiConfigDTO(pi.getRoomMonitoring().getRoomId(),
                    id,
                    pi.getFrequency(),
                    limitMapper.mapTo(pi.getRoomMonitoring()),
                    sensors,
                    occupancy != null ?
                    new OccupancyDTO(occupancy.getPeopleCnt(), occupancy.getRoomId(), occupancy.getPeopleCnt() < 5)
                    : null);
        } else {
            return new PiConfigDTO(null, id, pi.getFrequency(), null, null, null);
        }
    }

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
