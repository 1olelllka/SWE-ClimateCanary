package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.BuildingCreateDTO;
import at.qe.skeleton.dtos.BuildingDTO;
import at.qe.skeleton.dtos.BuildingListDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.BuildingDetailMapper;
import at.qe.skeleton.mappers.BuildingListMapper;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.services.BuildingService;
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
@RequestMapping("/buildings")
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

    @GetMapping("")
    public ResponseEntity<Page<BuildingListDTO>> getPageOfBuildings(Pageable pageable) {
        Page<Building> entities = buildingService.getAllBuildings(pageable);
        return new ResponseEntity<>(entities.map(buildingListMapper::mapTo), HttpStatus.OK);
    }

    @GetMapping("{building_id}")
    public ResponseEntity<BuildingDTO> getSpecificBuilding(@PathVariable(name="building_id") UUID id) {
        Building building = buildingService.getBuildingById(id);
        return new ResponseEntity<>(buildingDetailMapper.mapTo(building), HttpStatus.OK);
    }

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

    @PatchMapping("{building_id}")
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

    @DeleteMapping("{building_id}")
    public ResponseEntity<Void> deleteSpecificBuilding(@PathVariable(name="building_id") UUID id) {
        buildingService.deleteBuilding(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
