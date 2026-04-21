package at.qe.skeleton.services.impl;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.PiConfigDTO;
import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.NotifyDeadLetter;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.RoomOccupancy;
import at.qe.skeleton.repositories.NotifyDeadLetterRepository;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomOccupancyRepository;
import at.qe.skeleton.services.RaspberryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RaspberryServiceImpl implements RaspberryService {

    private final RaspberryPiRepository raspberryPiRepository;
    private final RoomMonitoringRepository monitoringRepository;
    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher;
    private final NotifyDeadLetterRepository deadLetterRepository;
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
            Optional.ofNullable(raspberryPi.getIp()).ifPresent(rasp::setIp);
            return raspberryPiRepository.save(rasp);
        }).orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
    }

    @Override
    @Transactional
    public void deleteRaspberry(UUID id) {
        Optional<RaspberryPi> optional = raspberryPiRepository.findById(id);
        if (optional.isPresent()) {
            optional.get().getRoomsMonitoring().forEach(r -> r.setRaspberryPi(null));
            monitoringRepository.saveAll(optional.get().getRoomsMonitoring());
            raspberryPiRepository.deleteById(id);
        }
    }

    @Override
    @Transactional
    public List<RoomOccupancy> getOccupancyFromRedis(UUID id) {
        RaspberryPi pi = raspberryPiRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
        List<UUID> roomIds = pi.getRoomsMonitoring().stream().map(RoomMonitoring::getRoomId).toList();
        return (List<RoomOccupancy>) occupancyRepository.findAllById(roomIds.stream().map(UUID::toString).collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public PiConfigDTO getConfigForRaspberry(UUID id) {
        RaspberryPi pi = raspberryPiRepository.findById(id).orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
        return new PiConfigDTO(pi.getFrequency(),
                pi.getRoomsMonitoring() != null
                        ? pi.getRoomsMonitoring().stream()
                                .filter(r -> r.getSensorStation() != null)
                                .map(r -> r.getSensorStation().getId()).collect(Collectors.toSet())
                        : null);
    }

    @Override
    @Transactional
    public void retryConnection(UUID raspberry_id) {
        RaspberryPi pi = raspberryPiRepository.findById(raspberry_id).orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + raspberry_id + " was not found."));
        if (pi.getRoomsMonitoring() != null) {
            for (RoomMonitoring monitoring : pi.getRoomsMonitoring()) {
                if (monitoring.getSensorStation() != null) {
                    eventPublisher.publishEvent(
                            new NotifyRaspberryCommand(
                                    new StateChangeNotificationDTO(UpdateType.SETUP, LocalDateTime.now()),
                                    monitoring.getSensorStation().getId(),
                                    pi, notificationClient));
                }
            }
            List<NotifyDeadLetter> letters = deadLetterRepository.findByRaspberryPi(pi.getId());
            letters.forEach(letter -> {
                    if (letter.getUpdateType() != UpdateType.SETUP) {
                    eventPublisher.publishEvent(
                            new NotifyRaspberryCommand(
                                    new StateChangeNotificationDTO(letter.getUpdateType(), letter.getTriggeredAt()),
                                    null, pi, notificationClient));
                }
            });
            deadLetterRepository.deleteAll(letters);
        } else {
            eventPublisher.publishEvent(
                    new NotifyRaspberryCommand(
                            new StateChangeNotificationDTO(UpdateType.SETUP, LocalDateTime.now()),
                            null, pi, notificationClient));
        }
    }

    @Override
    @Transactional
    public RaspberryPi addNewRoom(UUID raspberryId, UUID roomId) {
        RaspberryPi pi = raspberryPiRepository.findById(raspberryId)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + raspberryId + " was not found."));
        RoomMonitoring monitoring = monitoringRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
        if (monitoring.getRaspberryPi() != null) {
            throw new ConflictException("Room is already assigned to a Raspberry Pi.");
        }
        pi.addNewRoom(monitoring);
        monitoring.setRaspberryPi(pi);
        monitoringRepository.save(monitoring);
        return raspberryPiRepository.save(pi);
    }

    @Override
    @Transactional
    public RaspberryPi removeRoomFromRaspberry(UUID raspberryId, UUID roomId) {
        RaspberryPi pi = raspberryPiRepository.findById(raspberryId)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + raspberryId + " was not found."));
        RoomMonitoring monitoring = monitoringRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
        if (!pi.containsRoom(monitoring)) throw new NotFoundException("Cannot find this room inside of raspberry pi with id " + raspberryId);
        pi.removeRoom(monitoring);
        monitoring.setRaspberryPi(null);
        monitoringRepository.save(monitoring);
        return raspberryPiRepository.save(pi);
    }
}
