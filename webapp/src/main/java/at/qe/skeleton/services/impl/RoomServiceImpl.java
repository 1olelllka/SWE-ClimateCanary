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

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;
    private final RoomMonitoringRepository monitoringRepository;
    private final UserxRepository userxRepository;
    private final RaspberryPiRepository raspberryPiRepository;
    private final DepartmentRepository departmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationClient notificationClient;

    public Page<Room> getPageOfRooms(Pageable pageable) {return roomRepository.findAll(pageable);}

    @Transactional
    public Room createRoom(Room room) {
        if (!departmentRepository.existsById(room.getDepartment().getId())) throw new NotFoundException("Department with id " + room.getDepartment().getId() + " was not found.");
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
        monitoringRepository.save(monitoring);
        return r;
    }

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
                    Userx u = userxRepository.findById(user.getId()).orElseThrow(() -> new NotFoundException("User with id" + user.getId() + " was not found."));
                    u.setMyRoom(r);
                    foundUser.add(u);
                }
                r.setUsers(foundUser);
            }
            return roomRepository.save(r);
        }).orElseThrow(() -> new NotFoundException("Room not found with id: " + id));
    }

    @Override
    public RoomMonitoring getRoomMonitoring(UUID roomId) {
        return monitoringRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
    }

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
            return r;
        })
                .orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
    }

    @Override
    @Transactional
    public void deleteRoom(UUID id) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room != null) {
            room.getUsers().forEach(user -> {
                user.setMyRoom(null);
                userxRepository.save(user);
            });
        }
        roomRepository.deleteById(id);
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
        monitoringRepository.deleteById(id);
    }
}
