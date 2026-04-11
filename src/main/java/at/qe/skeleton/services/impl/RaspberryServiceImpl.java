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
import at.qe.skeleton.repositories.NotifyDeadLetterRepository;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.services.RaspberryService;
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

@Service
public class RaspberryServiceImpl implements RaspberryService {

    private final RaspberryPiRepository raspberryPiRepository;
    private final RoomMonitoringRepository monitoringRepository;
    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher;
    private final NotifyDeadLetterRepository deadLetterRepository;

    @Autowired
    public RaspberryServiceImpl(RaspberryPiRepository raspberryPiRepository,
                                RoomMonitoringRepository monitoringRepository,
                                NotificationClient notificationClient,
                                ApplicationEventPublisher eventPublisher,
                                NotifyDeadLetterRepository notifyDeadLetterRepository) {
        this.raspberryPiRepository = raspberryPiRepository;
        this.monitoringRepository = monitoringRepository;
        this.notificationClient = notificationClient;
        this.eventPublisher = eventPublisher;
        this.deadLetterRepository = notifyDeadLetterRepository;
    }

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
    public RaspberryPi createNewRaspberry(RaspberryPi raspberryPi, UUID roomId) {
        if (raspberryPiRepository.existsByName(raspberryPi.getName())) {
            throw new ConflictException("Raspberry Pi with name " + raspberryPi.getName() + " already exists.");
        }
        RoomMonitoring room = monitoringRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
        RaspberryPi toSave = raspberryPiRepository.save(raspberryPi);
        room.setRaspberryPi(raspberryPi);
        monitoringRepository.save(room);
        return toSave;
    }

    @Override
    public RaspberryPi updateRaspberryById(UUID id, RaspberryPi raspberryPi, UUID roomId) {
        return raspberryPiRepository.findById(id).map(rasp -> {
            Optional.ofNullable(raspberryPi.getFrequency()).ifPresent(rasp::setFrequency);
            Optional.ofNullable(raspberryPi.getName()).ifPresent(name -> {
                if (!rasp.getName().equals(name) && raspberryPiRepository.existsByName(name)) {
                    throw new ConflictException("Raspberry Pi with this name already exists.");
                }
                rasp.setName(name);
            });
            Optional.ofNullable(raspberryPi.getIp()).ifPresent(rasp::setIp);
            if (rasp.getRoomMonitoring() == null && roomId != null || roomId != null && !rasp.getRoomMonitoring().getRoomId().equals(roomId)) {
                RoomMonitoring monitoring = monitoringRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
                rasp.setRoomMonitoring(monitoring);
                monitoring.setRaspberryPi(rasp);
                monitoringRepository.save(monitoring);
            }
            return raspberryPiRepository.save(rasp);
        }).orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
    }

    @Override
    public void deleteRaspberry(UUID id) {
        Optional<RaspberryPi> optional = raspberryPiRepository.findById(id);
        if (optional.isPresent()) {
            optional.get().getRoomMonitoring().setRaspberryPi(null);
            monitoringRepository.save(optional.get().getRoomMonitoring());
            raspberryPiRepository.deleteById(id);
        }
    }

    @Override
    @Transactional
    public int getOccupancyFromRedis(UUID id) {
        RaspberryPi pi = raspberryPiRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
        UUID roomId = pi.getRoomMonitoring().getRoomId();
        // redisTemplate....
        return 10; // mocked value for now
    }

    @Override
    @Transactional
    public PiConfigDTO getConfigForRaspberry(UUID id) {
        RaspberryPi pi = raspberryPiRepository.findById(id).orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
        return new PiConfigDTO(pi.getFrequency(), pi.getRoomMonitoring() != null && pi.getRoomMonitoring().getSensorStation() != null ? pi.getRoomMonitoring().getSensorStation().getId() : null);
    }

    @Override
    @Transactional
    public void retryConnection(UUID id) {
        RaspberryPi pi = raspberryPiRepository.findById(id).orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
        eventPublisher.publishEvent(
                new NotifyRaspberryCommand(
                        new StateChangeNotificationDTO(UpdateType.SETUP, LocalDateTime.now()),
                        pi.getRoomMonitoring().getSensorStation().getId(), notificationClient));
        List<NotifyDeadLetter> letters = deadLetterRepository.findAll();
        letters.forEach(letter -> {
                if (letter.getUpdateType() != UpdateType.SETUP) {
                eventPublisher.publishEvent(
                        new NotifyRaspberryCommand(
                                new StateChangeNotificationDTO(letter.getUpdateType(), letter.getTriggeredAt()),
                                null, notificationClient));
            }
        });
        deadLetterRepository.deleteAll(letters);
    }
}
