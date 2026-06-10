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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
@Tag(name = "Department Management")
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

    @Operation(summary = "Get page of departments. One of Permissions Required: CAN_VIEW_ALL_BUILDINGS, CAN_MANAGE_BUILDING_STRUCTURE, CAN_VIEW_COMPANY_AGGR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of departments."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @GetMapping("")
    public ResponseEntity<Page<DepartmentListDTO>> getPageOfDepartments(Pageable pageable) {
        Page<Department> departments = departmentService.getPageOfDepartments(pageable);
        return new ResponseEntity<>(departments.map(departmentListMapper::mapTo), HttpStatus.OK);
    }

    @Operation(summary = "Get specific department. One of Permissions Required: CAN_VIEW_OWN_DEPARTMENT_MEASURES, CAN_VIEW_OWN_SHARED_CLIMATE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated absence."),
            @ApiResponse(responseCode = "404", description = "Department not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Auth failure.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
    })
    @GetMapping("/{department_id}")
    @PreAuthorize("hasAuthority('CAN_VIEW_OWN_DEPARTMENT_MEASURES') or hasAuthority('CAN_VIEW_OWN_SHARED_CLIMATE')")
    public ResponseEntity<DepartmentDTO> getSpecificDepartment(@PathVariable(name = "department_id") UUID id) {
        Department department = departmentService.getDepartmentById(id);
        return new ResponseEntity<>(departmentDetailMapper.mapTo(department), HttpStatus.OK);
    }

    @Operation(summary = "Create new department. One of Permissions Required: CAN_MANAGE_BUILDING_STRUCTURE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created new department."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Name inside of a building is duplicate.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
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

    @Operation(summary = "Update specific department. One of Permissions Required: CAN_MANAGE_BUILDING_STRUCTURE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated specific department."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Department/Room not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflicting names/rooms.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
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

    @Operation(summary = "Delete specific department. Deletes corresponding rooms too. One of Permissions Required: CAN_MANAGE_BUILDING_STRUCTURE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted Department."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @DeleteMapping("{department_id}")
    public ResponseEntity<Void> deleteSpecificDepartment(@PathVariable(name = "department_id") UUID id) {
        departmentService.deleteDepartment(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
