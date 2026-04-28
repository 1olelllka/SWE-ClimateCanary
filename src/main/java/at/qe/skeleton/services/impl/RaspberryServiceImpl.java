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
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomOccupancyRepository;
import at.qe.skeleton.services.RaspberryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
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
@Log
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
        log.info("Added new Raspberry Pi");
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
                log.info("Changed name of Raspberry Pi.");
                rasp.setName(name);
            });
            Optional.ofNullable(raspberryPi.getIp()).ifPresent(ip -> {
                if (raspberryPiRepository.existsByIpAndPort(ip, rasp.getPort())) {
                    throw new ConflictException("Raspberry Pi with this ip and port already exists.");
                }
                log.info("Changed IP of Raspberry Pi.");
                rasp.setIp(ip);
            });
            Optional.ofNullable(raspberryPi.getPort()).ifPresent(port -> {
                if (raspberryPiRepository.existsByIpAndPort(rasp.getIp(), port)) {
                    throw new ConflictException("Raspberry Pi with this ip and port already exists.");
                }
                log.info("Changed port of Raspberry Pi.");
                rasp.setPort(port);
            });
            log.info("Updated Raspberry Pi.");
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
                log.info("Deleting Raspberry Pi from the room...");
                pi.getRoomMonitoring().setRaspberryPi(null);
                monitoringRepository.save(pi.getRoomMonitoring());
            }
            raspberryPiRepository.deleteById(id);
            log.info("Raspberry Pi deleted.");
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
        Set<ReducedSensorDTO> sensors = pi.getRoomMonitoring() != null && pi.getRoomMonitoring().getSensorStations() != null
                ? pi.getRoomMonitoring().getSensorStations().stream()
                        .map(sensor -> new ReducedSensorDTO(sensor.getName(), sensor.getReadId(), sensor.getWriteId()))
                        .collect(Collectors.toSet())
                : Set.of();
        if (pi.getRoomMonitoring() != null) {
            return new PiConfigDTO(pi.getRoomMonitoring().getRoomId(), id, pi.getFrequency(), limitMapper.mapTo(pi.getRoomMonitoring()), sensors);
        } else {
            return new PiConfigDTO(null, id, pi.getFrequency(), null, null);
        }
    }

    @Override
    @Transactional
    public void retryConnection(UUID raspberry_id) {
        RaspberryPi pi = raspberryPiRepository.findById(raspberry_id)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + raspberry_id + " was not found."));
        log.info("Retrying connection with Raspberry Pi.");
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
        monitoring.setRaspberryPi(pi);
        monitoringRepository.save(monitoring);
        log.info("Making Raspberry Pi to check new config...");
        eventPublisher.publishEvent(
                new NotifyRaspberryCommand(
                        new ConfigRequestDTO(pi.getId(), LocalDateTime.now()),
                        pi,
                        notificationClient));
        log.info("Added new room for Raspberry Pi.");
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
        log.info("Making Raspberry Pi to check new config...");
        eventPublisher.publishEvent(
                new NotifyRaspberryCommand(
                        new ConfigRequestDTO(pi.getId(), LocalDateTime.now()),
                        pi,
                        notificationClient));
        log.info("Removed room from Raspberry Pi.");
        return raspberryPiRepository.save(pi);
    }
}
