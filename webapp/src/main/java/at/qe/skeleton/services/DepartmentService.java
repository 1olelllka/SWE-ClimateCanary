package at.qe.skeleton.services;

import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DepartmentService {
    Page<Department> getPageOfDepartments(Pageable pageable);

    Department createDepartment(Department department);

    Department createDepartmentWithRooms(Department department, List<UUID> existingRoomIds, List<Room> newRooms);

    Department editDepartmentWithRooms(UUID deptId, Department updatedDept, List<UUID> roomIdsToDelete, List<UUID> existingRoomIdsToAssign, List<Room> newRooms);

    void deleteDepartment(UUID id);

    Department patchSpecificDepartment(UUID id, Department department);

    Department getDepartmentById(UUID id);
}
