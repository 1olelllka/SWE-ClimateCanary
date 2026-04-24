package at.qe.skeleton.services.impl;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
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
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationClient notificationClient;

    public Page<Room> getPageOfRooms(Pageable pageable) {return roomRepository.findAll(pageable);}

//    public Room getRoomById(UUID id){
//        return roomRepository.findById(id)
//                .orElseThrow(() -> new NotFoundException("Room not found with id: " + id));
//    }

    @Transactional
    public Room createRoom(Room room) {
        if (roomRepository.existsByRoomNumber(room.getRoomNumber())) {
            throw new ConflictException("Room with such name already exists.");
        }
        Room r = roomRepository.save(room);
        monitoringRepository.save(RoomMonitoring.builder().roomId(r.getId()).roomNumber(r.getRoomNumber()).build());
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
                if (roomRepository.existsByRoomNumber(room.getRoomNumber())) {
                    throw new ConflictException("Room with such name already exists.");
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
    @Transactional
    public void deleteRoom(UUID id) {
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
//
//    public Department getDepartmentOfRoom(UUID roomId) {
//        Room room = getRoomById(roomId);
//        return room.getDepartment();
//    }
}
