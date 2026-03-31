package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.DepartmentCreateDTO;
import at.qe.skeleton.dtos.DepartmentDTO;
import at.qe.skeleton.dtos.DepartmentListDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.DepartmentDetailMapper;
import at.qe.skeleton.mappers.DepartmentListMapper;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.services.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/departments")
public class DepartmentController {

    private DepartmentService departmentService;
    private DepartmentDetailMapper departmentDetailMapper;
    private DepartmentListMapper departmentListMapper;

    @Autowired
    public DepartmentController(DepartmentService departmentService,
                                DepartmentDetailMapper departmentDetailMapper,
                                DepartmentListMapper departmentListMapper) {
        this.departmentDetailMapper = departmentDetailMapper;
        this.departmentService = departmentService;
        this.departmentListMapper = departmentListMapper;
    }

    @GetMapping("")
    public ResponseEntity<Page<DepartmentListDTO>> getPageOfDepartments(Pageable pageable) {
        Page<Department> departments = departmentService.getPageOfDepartments(pageable);
        return new ResponseEntity<>(departments.map(departmentListMapper::mapTo), HttpStatus.OK);
    }

    @GetMapping("{department_id}")
    public ResponseEntity<DepartmentDTO> getSpecificDepartment(@PathVariable(name = "department_id") UUID id) {
        Department department = departmentService.getDepartmentById(id);
        return new ResponseEntity<>(departmentDetailMapper.mapTo(department), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<DepartmentDTO> createNewDepartment(@RequestBody @Valid DepartmentCreateDTO dto,
                                                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Department created = departmentService
                .createDepartment(
                        Department
                                .builder()
                                .name(dto.name())
                                .building(Building.builder().id(dto.buildingId()).build())
                                .build());
        return new ResponseEntity<>(departmentDetailMapper.mapTo(created), HttpStatus.CREATED);
    }

    @PatchMapping("{department_id}")
    public ResponseEntity<DepartmentDTO> patchSpecificDepartment(@PathVariable(name="department_id") UUID id,
                                                                 @RequestBody @Valid DepartmentCreateDTO dto,
                                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Department patched = departmentService.patchSpecificDepartment(id,
                Department.builder()
                        .name(dto.name())
                        .building(Building.builder().id(dto.buildingId()).build())
                        .build());
        return new ResponseEntity<>(departmentDetailMapper.mapTo(patched), HttpStatus.OK);
    }

    @DeleteMapping("{department_id}")
    public ResponseEntity<Void> deleteSpecificDepartment(@PathVariable(name = "department_id") UUID id) {
        departmentService.deleteDepartment(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
