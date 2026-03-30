package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.DepartmentDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.DepartmentMapper;
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
    private DepartmentMapper departmentMapper;

    @Autowired
    public DepartmentController(DepartmentService departmentService,
                                DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
        this.departmentService = departmentService;
    }

    @GetMapping("")
    public ResponseEntity<Page<DepartmentDTO>> getPageOfDepartments(Pageable pageable) {
        Page<Department> departments = departmentService.getPageOfDepartments(pageable);
        return new ResponseEntity<>(departments.map(departmentMapper::mapTo), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<DepartmentDTO> createNewDepartment(@RequestBody @Valid DepartmentDTO dto,
                                                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Department created = departmentService.createDepartment(departmentMapper.mapFrom(dto));
        return new ResponseEntity<>(departmentMapper.mapTo(created), HttpStatus.CREATED);
    }

    @PatchMapping("{department_id}")
    public ResponseEntity<DepartmentDTO> patchSpecificDepartment(@PathVariable(name="department_id") UUID id,
                                                                 @RequestBody @Valid DepartmentDTO dto,
                                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Department patched = departmentService.patchSpecificDepartment(id, departmentMapper.mapFrom(dto));
        return new ResponseEntity<>(departmentMapper.mapTo(patched), HttpStatus.OK);
    }

    @DeleteMapping("{department_id}")
    public ResponseEntity<Void> deleteSpecificDepartment(@PathVariable(name = "department_id") UUID id) {
        departmentService.deleteDepartment(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
