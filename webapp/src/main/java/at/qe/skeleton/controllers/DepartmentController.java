package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.DepartmentDTO;
import at.qe.skeleton.dtos.DepartmentEditWithRoomsDTO;
import at.qe.skeleton.dtos.DepartmentListDTO;
import at.qe.skeleton.dtos.DepartmentWithRoomsCreateDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.DepartmentDetailMapper;
import at.qe.skeleton.mappers.DepartmentListMapper;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.services.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
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

    @GetMapping("/{department_id}")
    @PreAuthorize("hasAuthority('CAN_VIEW_OWN_DEPARTMENT_MEASURES') or hasAuthority('CAN_VIEW_OWN_SHARED_CLIMATE')")
    public ResponseEntity<DepartmentDTO> getSpecificDepartment(@PathVariable(name = "department_id") UUID id) {
        Department department = departmentService.getDepartmentById(id);
        return new ResponseEntity<>(departmentDetailMapper.mapTo(department), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<DepartmentDTO> createNewDepartment(@RequestBody @Valid DepartmentWithRoomsCreateDTO dto,
                                                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        List<UUID> existingRoomIds = dto.existingRoomIds() != null ? dto.existingRoomIds() : List.of();
        List<Room> newRooms = dto.newRooms() != null ? dto.newRooms().stream()
                .map(r -> Room.builder()
                        .roomNumber(r.name())
                        .roomType(r.roomType())
                        .defaultPeopleCnt(r.defaultPeopleCount())
                        .isActive(true)
                        .build())
                .toList() : List.of();
        Department created = departmentService.createDepartmentWithRooms(
                Department.builder()
                        .name(dto.name())
                        .building(Building.builder().id(dto.buildingID()).build())
                        .build(),
                existingRoomIds,
                newRooms);
        return new ResponseEntity<>(departmentDetailMapper.mapTo(created), HttpStatus.CREATED);
    }

    @PatchMapping("{department_id}")
    public ResponseEntity<DepartmentDTO> patchSpecificDepartment(@PathVariable(name="department_id") UUID id,
                                                                 @RequestBody @Valid DepartmentEditWithRoomsDTO dto,
                                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        List<UUID> roomIdsToDelete = dto.roomIdsToDelete() != null ? dto.roomIdsToDelete() : List.of();
        List<UUID> existingRoomIdsToAssign = dto.existingRoomIdsToAssign() != null ? dto.existingRoomIdsToAssign() : List.of();
        List<Room> newRooms = dto.newRooms() != null ? dto.newRooms().stream()
                .map(r -> Room.builder()
                        .roomNumber(r.name())
                        .roomType(r.roomType())
                        .defaultPeopleCnt(r.defaultPeopleCount())
                        .isActive(true)
                        .build())
                .toList() : List.of();
        Department patched = departmentService.editDepartmentWithRooms(id,
                Department.builder()
                        .name(dto.name())
                        .building(Building.builder().id(dto.buildingID()).build())
                        .build(),
                roomIdsToDelete, existingRoomIdsToAssign, newRooms);
        return new ResponseEntity<>(departmentDetailMapper.mapTo(patched), HttpStatus.OK);
    }

    @DeleteMapping("{department_id}")
    public ResponseEntity<Void> deleteSpecificDepartment(@PathVariable(name = "department_id") UUID id) {
        departmentService.deleteDepartment(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
