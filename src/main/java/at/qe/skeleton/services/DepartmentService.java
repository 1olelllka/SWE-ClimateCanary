package at.qe.skeleton.services;


import at.qe.skeleton.exceptions.ResourceNotFoundException;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.repositories.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository){
        this.departmentRepository = departmentRepository;
    }

    public List<Department> getAllDepartments() {return departmentRepository.findAll();}

    public Department getDepartmentById(UUID id){
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    public Department getDepartmentByName(String name){
        return departmentRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with name: " + name));
    }

    public Department createDepartment(Department department) {

        if (departmentRepository.findByName(department.getName()).isPresent()) {
            throw new IllegalArgumentException("Department with this name already exists");
        }

        return departmentRepository.save(department);
    }

    public void deleteDepartment(UUID id) {
        departmentRepository.deleteById(id);
    }

    public List<Room> getRoomsByDepartment(UUID departmentId) {
        Department department = getDepartmentById(departmentId); // in DepartmentService
        return department.getRooms();
    }

    public Building getBuildingOfDepartment(UUID departmentId) {
        Department department = getDepartmentById(departmentId);
        return department.getBuilding();
    }
}
