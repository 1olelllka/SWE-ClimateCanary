package at.qe.skeleton.services.impl;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.PiConfigDTO;
import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.RoomOccupancy;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomOccupancyRepository;
import at.qe.skeleton.services.RaspberryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RaspberryServiceImpl implements RaspberryService {

    private final RaspberryPiRepository raspberryPiRepository;
    private final RoomMonitoringRepository monitoringRepository;
    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomOccupancyRepository occupancyRepository;

    @Override
    public Page<RaspberryPi> getAllRaspberries(Pageable pageable) {
        return raspberryPiRepository.findAll(pageable);
    }

    @Override
    public RaspberryPi getSpecificRaspberry(UUID id) {
        return raspberryPiRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi device with id " + id + " was not found."));
    }

    @Override
    public RaspberryPi createNewRaspberry(RaspberryPi raspberryPi) {
        if (raspberryPiRepository.existsByName(raspberryPi.getName())) {
            throw new ConflictException("Raspberry Pi with name " + raspberryPi.getName() + " already exists.");
        }
        if (raspberryPiRepository.existsByIpAndPort(raspberryPi.getIp(), raspberryPi.getPort())) {
            throw new ConflictException("Raspberry Pi with this ip and port already exists.");
        }
        return raspberryPiRepository.save(raspberryPi);
    }

    @Override
    public RaspberryPi updateRaspberryById(UUID id, RaspberryPi raspberryPi) {
        return raspberryPiRepository.findById(id).map(rasp -> {
            Optional.ofNullable(raspberryPi.getFrequency()).ifPresent(rasp::setFrequency);
            Optional.ofNullable(raspberryPi.getName()).ifPresent(name -> {
                if (!rasp.getName().equals(name) && raspberryPiRepository.existsByName(name)) {
                    throw new ConflictException("Raspberry Pi with this name already exists.");
                }
                rasp.setName(name);
            });
            Optional.ofNullable(raspberryPi.getIp()).ifPresent(ip -> {
                if (raspberryPiRepository.existsByIpAndPort(ip, rasp.getPort())) {
                    throw new ConflictException("Raspberry Pi with this ip and port already exists.");
                }
                rasp.setIp(ip);
            });
            Optional.ofNullable(raspberryPi.getPort()).ifPresent(port -> {
                if (raspberryPiRepository.existsByIpAndPort(rasp.getIp(), port)) {
                    throw new ConflictException("Raspberry Pi with this ip and port already exists.");
                }
                rasp.setPort(port);
            });
            return raspberryPiRepository.save(rasp);
        }).orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
    }

    @Override
    @Transactional
    public void deleteRaspberry(UUID id) {
        Optional<RaspberryPi> optional = raspberryPiRepository.findById(id);
        if (optional.isPresent()) {
            RaspberryPi pi = optional.get();
            if (pi.getRoomMonitoring() != null) {
                pi.getRoomMonitoring().setRaspberryPi(null);
                monitoringRepository.save(pi.getRoomMonitoring());
            }
            raspberryPiRepository.deleteById(id);
        }
    }

    @Override
    @Transactional
    public List<RoomOccupancy> getOccupancyFromRedis(UUID id) {
        RaspberryPi pi = raspberryPiRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
        if (pi.getRoomMonitoring() == null) return List.of();
        return (List<RoomOccupancy>) occupancyRepository.findAllById(
                List.of(pi.getRoomMonitoring().getRoomId().toString()));
    }

    @Override
    @Transactional
    public PiConfigDTO getConfigForRaspberry(UUID id) {
        RaspberryPi pi = raspberryPiRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
        Set<UUID> sensors = pi.getRoomMonitoring() != null && pi.getRoomMonitoring().getSensorStations() != null
                ? pi.getRoomMonitoring().getSensorStations().stream()
                        .map(SensorStation::getReadId)
                        .collect(Collectors.toSet())
                : Set.of();
        return new PiConfigDTO(pi.getFrequency(), sensors);
    }

    @Override
    @Transactional
    public void retryConnection(UUID raspberry_id) {
        RaspberryPi pi = raspberryPiRepository.findById(raspberry_id)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + raspberry_id + " was not found."));
        eventPublisher.publishEvent(
                new NotifyRaspberryCommand(
                        new StateChangeNotificationDTO(UpdateType.CONFIG, LocalDateTime.now()),
                        null, null, pi, notificationClient));
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
        monitoring.setRaspberryPi(pi);
        monitoringRepository.save(monitoring);
        eventPublisher.publishEvent(
                new NotifyRaspberryCommand(
                        new StateChangeNotificationDTO(UpdateType.CONFIG, LocalDateTime.now()),
                        null,
                        null,
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
        eventPublisher.publishEvent(
                new NotifyRaspberryCommand(
                        new StateChangeNotificationDTO(UpdateType.CONFIG, LocalDateTime.now()),
                        null,
                        null,
                        pi,
                        notificationClient));
        return raspberryPiRepository.save(pi);
    }
}
