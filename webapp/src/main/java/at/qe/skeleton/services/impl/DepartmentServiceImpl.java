package at.qe.skeleton.services.impl;


import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final RoomMonitoringRepository monitoringRepository;
    private final RaspberryPiRepository raspberryPiRepository;
    private final BuildingTrendRepository trendRepository;
    private final RoomServiceImpl roomService;
    private final AuthenticatedUserService authenticatedUserService;

    public Page<Department> getPageOfDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable);
    }

    public Department getDepartmentById(UUID id) {
        Userx authenticated = authenticatedUserService.getAuthenticatedUser();
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + id));
        if (!authenticated.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList().contains(Permission.CAN_VIEW_OWN_DEPARTMENT_MEASURES.name())) {
            department.setRooms(department.getRooms().stream().filter(room -> room.getRoomType().equals(RoomType.SHARED)).toList());
        }
        return department;
    }

    public Department createDepartment(Department department) {
        if (departmentRepository.existsByNameAndBuildingId(department.getName(), department.getBuilding().getId())) {
            throw new ConflictException("Department with this name already exists");
        }
        return departmentRepository.save(department);
    }

    @Transactional
    public Department createDepartmentWithRooms(Department department, List<UUID> existingRoomIds, List<Room> newRooms) {
        if (departmentRepository.existsByNameAndBuildingId(department.getName(), department.getBuilding().getId())) {
            throw new ConflictException("Department with this name already exists");
        }
        Department created = departmentRepository.save(department);

        for (UUID roomId : existingRoomIds) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new NotFoundException("Room with id %s was not found.".formatted(roomId)));
            if (roomRepository.existsByRoomNumberAndDepartmentId(room.getRoomNumber(), created.getId())) {
                throw new ConflictException("Room '" + room.getRoomNumber() + "' already exists in this department.");
            }
            room.setDepartment(created);
            roomRepository.save(room);
            created.addNewRoom(room);
        }

        for (Room newRoom : newRooms) {
            newRoom.setDepartment(created);
            newRoom = roomService.createRoom(newRoom);
            created.addNewRoom(newRoom);
        }

        return created;
    }

    @Transactional
    public Department editDepartmentWithRooms(UUID deptId, Department updatedDept,
                                              List<UUID> roomIdsToDelete,
                                              List<UUID> existingRoomIdsToAssign,
                                              List<Room> newRooms) {
        Department dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + deptId));

        if (!updatedDept.getName().equals(dept.getName()) &&
                departmentRepository.existsByNameAndBuildingId(updatedDept.getName(), dept.getBuilding().getId())) {
            throw new ConflictException("Department with this name already exists");
        }
        dept.setName(updatedDept.getName());
        if (updatedDept.getBuilding() != null) dept.setBuilding(updatedDept.getBuilding());
        departmentRepository.save(dept);

        for (UUID roomId : roomIdsToDelete) {
            RoomMonitoring monitoring = monitoringRepository.findById(roomId).orElse(null);
            if (monitoring != null && monitoring.getRaspberryPi() != null) {
                monitoring.getRaspberryPi().setRoomMonitoring(null);
                raspberryPiRepository.save(monitoring.getRaspberryPi());
            }
            monitoringRepository.deleteById(roomId);
            roomRepository.deleteById(roomId);
        }

        for (UUID roomId : existingRoomIdsToAssign) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
            if (roomRepository.existsByRoomNumberAndDepartmentId(room.getRoomNumber(), dept.getId())) {
                throw new ConflictException("Room '" + room.getRoomNumber() + "' already exists in this department.");
            }
            room.setDepartment(dept);
            roomRepository.save(room);
        }

        for (Room newRoom : newRooms) {
            newRoom.setDepartment(dept);
            if (roomRepository.existsByRoomNumberAndDepartmentId(newRoom.getRoomNumber(), dept.getId())) {
                throw new ConflictException("Room '" + newRoom.getRoomNumber() + "' already exists in this department.");
            }
            Room saved = roomRepository.save(newRoom);
            monitoringRepository.save(RoomMonitoring.builder()
                    .roomId(saved.getId())
                    .roomNumber(saved.getRoomNumber())
                    .humLimit(HumidityLimit.builder().build())
                    .tempLimit(TemperatureLimit.builder().build())
                    .polLimit(PollutionLimit.builder().build())
                    .build());
        }

        return dept;
    }

    public void deleteDepartment(UUID id) {
        departmentRepository.deleteById(id);
        trendRepository.deleteAllByDepartmentId(id);
    }

    @Override
    public Department patchSpecificDepartment(UUID id, Department department) {
        return departmentRepository.findById(id).map(dep -> {
            Optional.ofNullable(department.getName()).ifPresent(name -> {
                if (!name.equals(dep.getName()) && departmentRepository.existsByNameAndBuildingId(name, dep.getBuilding().getId()))
                    throw new ConflictException("Department with the same name exists");
                dep.setName(name);
            });
            Optional.ofNullable(department.getBuilding()).ifPresent(dep::setBuilding);
            return departmentRepository.save(dep);
        }).orElseThrow(() -> new NotFoundException("Department with id %s was not found".formatted(department.getId())));
    }
}
