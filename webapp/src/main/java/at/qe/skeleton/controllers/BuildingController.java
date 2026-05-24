package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.BuildingCreateDTO;
import at.qe.skeleton.dtos.BuildingDTO;
import at.qe.skeleton.dtos.BuildingListDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.BuildingDetailMapper;
import at.qe.skeleton.mappers.BuildingListMapper;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.services.BuildingService;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/buildings")
@Tag(name = "Building Management")
public class BuildingController {

    private BuildingService buildingService;
    private BuildingDetailMapper buildingDetailMapper;
    private BuildingListMapper buildingListMapper;

    @Autowired
    public BuildingController(BuildingService buildingService,
                              BuildingListMapper buildingListMapper,
                              BuildingDetailMapper mapper) {
        this.buildingService = buildingService;
        this.buildingDetailMapper = mapper;
        this.buildingListMapper = buildingListMapper;
    }

    @Operation(summary = "Get page of buildings. Permissions Required: CAN_VIEW_ALL_BUILDINGS, CAN_MANAGE_BUILDING_STRUCTURE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of buildings."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @GetMapping("")
    public ResponseEntity<Page<BuildingListDTO>> getPageOfBuildings(Pageable pageable) {
        Page<Building> entities = buildingService.getAllBuildings(pageable);
        return new ResponseEntity<>(entities.map(buildingListMapper::mapTo), HttpStatus.OK);
    }

    @Operation(summary = "Get specific building. Permissions Required: CAN_VIEW_ALL_BUILDINGS, CAN_MANAGE_BUILDING_STRUCTURE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Specific building."),
            @ApiResponse(responseCode = "404", description = "Building not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @GetMapping("/{building_id}")
    public ResponseEntity<BuildingDTO> getSpecificBuilding(@PathVariable(name="building_id") UUID id) {
        Building building = buildingService.getBuildingById(id);
        return new ResponseEntity<>(buildingDetailMapper.mapTo(building), HttpStatus.OK);
    }

    @Operation(summary = "Create new building. Permissions Required: CAN_MANAGE_BUILDING_STRUCTURE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created Building."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Either name or address is duplicate.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @PostMapping("")
    public ResponseEntity<BuildingDTO> createNewBuilding(@RequestBody @Valid BuildingCreateDTO dto,
                                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Building created = buildingService
                .createBuilding(Building.builder().name(dto.name()).address(dto.address()).build());
        return new ResponseEntity<>(buildingDetailMapper.mapTo(created), HttpStatus.CREATED);
    }

    @Operation(summary = "Update specific building. Permissions Required: CAN_MANAGE_BUILDING_STRUCTURE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated building."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Building not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Either name or address is duplicate.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @PatchMapping("/{building_id}")
    public ResponseEntity<BuildingDTO> patchSpecificBuilding(@PathVariable(name = "building_id") UUID id,
                                                             @RequestBody @Valid BuildingCreateDTO dto,
                                                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Building patched = buildingService.patchSpecificBuilding(id,
                Building.builder().name(dto.name()).address(dto.address()).build());
        return new ResponseEntity<>(buildingDetailMapper.mapTo(patched), HttpStatus.OK);
    }

    @Operation(summary = "Delete specific building. Deletes departments and rooms too. Permissions Required: CAN_MANAGE_BUILDING_STRUCTURE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @DeleteMapping("/{building_id}")
    public ResponseEntity<Void> deleteSpecificBuilding(@PathVariable(name="building_id") UUID id) {
        buildingService.deleteBuilding(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
