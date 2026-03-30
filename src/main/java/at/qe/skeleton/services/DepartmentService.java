package at.qe.skeleton.services;

import at.qe.skeleton.model.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DepartmentService {
    Page<Department> getPageOfDepartments(Pageable pageable);

    Department createDepartment(Department department);

    void deleteDepartment(UUID id);

    Department patchSpecificDepartment(UUID id, Department department);
}
