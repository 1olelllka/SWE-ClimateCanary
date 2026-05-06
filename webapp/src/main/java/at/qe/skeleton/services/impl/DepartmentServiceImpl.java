package at.qe.skeleton.services.impl;


import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.services.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final RoomMonitoringRepository monitoringRepository;
    private final RaspberryPiRepository raspberryPiRepository;

    @Autowired
    public DepartmentServiceImpl(DepartmentRepository departmentRepository,
                                 RoomRepository roomRepository,
                                 RoomMonitoringRepository monitoringRepository,
                                 RaspberryPiRepository raspberryPiRepository) {
        this.departmentRepository = departmentRepository;
        this.roomRepository = roomRepository;
        this.monitoringRepository = monitoringRepository;
        this.raspberryPiRepository = raspberryPiRepository;
    }

    public Page<Department> getPageOfDepartments(Pageable pageable) {return departmentRepository.findAll(pageable);}

    public Department getDepartmentById(UUID id){
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + id));
    }

//    public Department getDepartmentByName(String name){
//        return departmentRepository.findByName(name)
//                .orElseThrow(() -> new NotFoundException("Department not found with name: " + name));
//    }

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
                    .orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
            if (roomRepository.existsByRoomNumberAndDepartmentId(room.getRoomNumber(), created.getId())) {
                throw new ConflictException("Room '" + room.getRoomNumber() + "' already exists in this department.");
            }
            room.setDepartment(created);
            roomRepository.save(room);
        }

        for (Room newRoom : newRooms) {
            newRoom.setDepartment(created);
            if (roomRepository.existsByRoomNumberAndDepartmentId(newRoom.getRoomNumber(), created.getId())) {
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
    }

    @Override
    public Department patchSpecificDepartment(UUID id, Department department) {
        return departmentRepository.findById(id).map(dep -> {
            Optional.ofNullable(department.getName()).ifPresent(name -> {
                if (!name.equals(dep.getName()) && departmentRepository.existsByNameAndBuildingId(name, dep.getBuilding().getId())) throw new ConflictException("Department with the same name exists");
                dep.setName(name);
            });
            Optional.ofNullable(department.getBuilding()).ifPresent(dep::setBuilding);
            return departmentRepository.save(dep);
        }).orElseThrow(() -> new NotFoundException("Department with such id " + department.getId() + " was not found."));
    }

//    public List<Room> getRoomsByDepartment(UUID departmentId) {
//        Department department = getDepartmentById(departmentId); // in DepartmentService
//        return department.getRooms();
//    }
//
//    public Building getBuildingOfDepartment(UUID departmentId) {
//        Department department = getDepartmentById(departmentId);
//        return department.getBuilding();
//    }
}
