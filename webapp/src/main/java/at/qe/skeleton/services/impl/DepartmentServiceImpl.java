package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link DepartmentService} providing CRUD operations for
 * {@link Department} entities. Room visibility is restricted based on the
 * authenticated user's permissions: users without
 * {@code CAN_VIEW_OWN_DEPARTMENT_MEASURES} see only {@link RoomType#SHARED} rooms
 * when fetching a single department. Deletion cascades through all rooms via
 * {@link RoomServiceImpl#deleteRoom}, then cleans up trend and aggregated stats records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final AggregatedDepartmentStatsRepository departmentStatsRepository;
    private final RoomRepository roomRepository;
    private final RoomMonitoringRepository monitoringRepository;
    private final RaspberryPiRepository raspberryPiRepository;
    private final BuildingTrendRepository trendRepository;
    private final RoomServiceImpl roomService;
    private final AuthenticatedUserService authenticatedUserService;

    /**
     * Returns a paginated list of all departments.
     *
     * @param pageable pagination parameters
     * @return page of {@link Department} entities
     */
    public Page<Department> getPageOfDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable);
    }

    /**
     * Returns the department with the given ID. If the authenticated user does not
     * hold the {@code CAN_VIEW_OWN_DEPARTMENT_MEASURES} permission, the room list is
     * filtered to include only {@link RoomType#SHARED} rooms.
     *
     * @param id the department UUID
     * @return the matching {@link Department}, with rooms filtered by access level
     * @throws NotFoundException if no department with that ID exists
     */
    public Department getDepartmentById(UUID id) {
        Userx authenticated = authenticatedUserService.getAuthenticatedUser();
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + id));
        if (!authenticated.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList().contains(Permission.CAN_VIEW_OWN_DEPARTMENT_MEASURES.name())) {
            department.setRooms(department.getRooms().stream().filter(room -> room.getRoomType().equals(RoomType.SHARED)).toList());
        }
        return department;
    }

    /**
     * Creates and persists a new department (no rooms). The name must be unique within
     * the department's building.
     *
     * @param department the department to create
     * @return the saved {@link Department}
     * @throws ConflictException if a department with the same name already exists in that building
     */
    public Department createDepartment(Department department) {
        if (departmentRepository.existsByNameAndBuildingId(department.getName(), department.getBuilding().getId())) {
            throw new ConflictException("Department with this name already exists");
        }
        log.info("Created new department {}", department.getName());
        return departmentRepository.save(department);
    }

    /**
     * Creates a new department and populates it with a mix of existing rooms
     * (reassigned by UUID) and newly created rooms. Room number uniqueness within the
     * department is validated for both sets.
     *
     * @param department      the department to create
     * @param existingRoomIds UUIDs of pre-existing rooms to reassign to this department
     * @param newRooms        new {@link Room} instances to create and assign
     * @return the saved {@link Department} with all rooms attached
     * @throws ConflictException if the department name is taken in the building, or if
     *                           any room number already exists in the new department
     * @throws NotFoundException if any of the existing room IDs cannot be found
     */
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
        log.info("Created new department {} with {} rooms", department.getName(), department.getRooms().size());
        return created;
    }

    /**
     * Edits a department's name, building, and room composition in a single transaction.
     * Rooms in {@code roomIdsToDelete} are fully removed (their Raspberry Pi references
     * are cleared first). Rooms in {@code existingRoomIdsToAssign} are re-parented to
     * this department. Rooms in {@code newRooms} are created with default monitoring limits.
     *
     * @param deptId                  the UUID of the department to edit
     * @param updatedDept             the new name and optional building reference
     * @param roomIdsToDelete         UUIDs of rooms to delete from the system
     * @param existingRoomIdsToAssign UUIDs of pre-existing rooms to move into this department
     * @param newRooms                new {@link Room} instances to create and assign
     * @return the updated {@link Department}
     * @throws NotFoundException if the department or any referenced room cannot be found
     * @throws ConflictException if the new department name is already taken, or if any
     *                           room number conflicts within the department
     */
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

    /**
     * Deletes the department with the given ID along with all its rooms (via
     * {@link RoomServiceImpl#deleteRoom}), trend records, and aggregated department
     * stats. If no department with that ID exists, only the stats cleanup is performed.
     *
     * @param id the UUID of the department to delete
     */
    @Transactional
    public void deleteDepartment(UUID id) {
        Department department = departmentRepository.findById(id).orElse(null);
        if (department != null) {
            List<UUID> roomIds = department.getRooms()
                    .stream()
                    .map(Room::getId)
                    .toList();
            roomIds.forEach(roomService::deleteRoom);
        }
        log.info("Deleted department {} - {}", id, department != null ? department.getName() : "{UNKNOWN}");
        if (department != null) departmentRepository.delete(department);
        trendRepository.deleteAllByDepartmentId(id);
        departmentStatsRepository.deleteAllByDepartmentId(id);
    }
}