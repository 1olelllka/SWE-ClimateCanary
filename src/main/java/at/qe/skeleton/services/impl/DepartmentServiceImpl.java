package at.qe.skeleton.services.impl;


import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.services.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;

    @Autowired
    public DepartmentServiceImpl(DepartmentRepository departmentRepository){
        this.departmentRepository = departmentRepository;
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
