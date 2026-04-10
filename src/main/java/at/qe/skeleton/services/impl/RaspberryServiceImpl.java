package at.qe.skeleton.services.impl;

import at.qe.skeleton.dtos.PiConfigDTO;
import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.services.RaspberryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RaspberryServiceImpl implements RaspberryService {

    private final RaspberryPiRepository raspberryPiRepository;
    private final RoomMonitoringRepository monitoringRepository;
    private final NotificationClient notificationClient;

    @Autowired
    public RaspberryServiceImpl(RaspberryPiRepository raspberryPiRepository,
                                RoomMonitoringRepository monitoringRepository,
                                NotificationClient notificationClient) {
        this.raspberryPiRepository = raspberryPiRepository;
        this.monitoringRepository = monitoringRepository;
        this.notificationClient = notificationClient;
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
    public RaspberryPi createNewRaspberry(RaspberryPi raspberryPi) {
        if (raspberryPiRepository.existsByIp(raspberryPi.getIp())) {
            throw new ConflictException("Raspberry Pi with ip " + raspberryPi.getIp() + " already exists.");
        }
        if (raspberryPiRepository.existsByName(raspberryPi.getName())) {
            throw new ConflictException("Raspberry Pi with name " + raspberryPi.getName() + " already exists.");
        }
        RoomMonitoring room = raspberryPi.getRoomMonitoring();
        RaspberryPi toSave = raspberryPiRepository.save(raspberryPi);
        room.setRaspberryPi(raspberryPi);
        monitoringRepository.save(room);
        return toSave;
    }

    @Override
    public RaspberryPi updateRaspberryById(UUID id, RaspberryPi raspberryPi) {
        return raspberryPiRepository.findById(id).map(rasp -> {
            Optional.ofNullable(raspberryPi.getFrequency()).ifPresent(rasp::setFrequency);
            // TODO: Discussion – should ip be unique for raspberry?
            Optional.of(raspberryPi.getName()).ifPresent(name -> {
                if (!rasp.getName().equals(name) && raspberryPiRepository.existsByName(name)) {
                    throw new ConflictException("Raspberry Pi with this name already exists.");
                }
                rasp.setName(name);
            });
            Optional.of(raspberryPi.getIp()).ifPresent(ip -> {
                if (!ip.equals(rasp.getIp()) && raspberryPiRepository.existsByIp(ip)) {
                    throw new ConflictException("Raspberry Pi with IP " + ip + " already exists.");
                }
                rasp.setIp(ip);
            });
            Optional.of(raspberryPi.getRoomMonitoring()).ifPresent(room -> {
                rasp.setRoomMonitoring(room);
                room.setRaspberryPi(rasp);
                monitoringRepository.save(room);
            });
            return raspberryPiRepository.save(rasp);
                })
                .orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
    }

    @Override
    public void deleteRaspberry(UUID id) {
        raspberryPiRepository.deleteById(id);
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
        return new PiConfigDTO(pi.getFrequency(), pi.getRoomMonitoring().getSensorStation().getId());
    }

    @Override
    @Transactional
    public void retryConnection(UUID id) {
        RaspberryPi pi = raspberryPiRepository.findById(id).orElseThrow(() -> new NotFoundException("Raspberry Pi with id " + id + " was not found."));
        // Connection client: (configure the ip first)
        /*
        notificationClient
                .notifyRaspberryAboutChanges(new StateChangeNotificationDTO(UpdateType.SETUP, LocalDateTime.now()),
                        pi.getRoomMonitoring().getSensorStation().getId());
         */
    }
}
