package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.services.BuildingService;
import at.qe.skeleton.services.DepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class BuildingServiceImpl implements BuildingService {

    private final BuildingRepository buildingRepository;
    private final DepartmentService departmentService;

    @Autowired
    public BuildingServiceImpl(BuildingRepository buildingRepository,
                               DepartmentService departmentService) {
        this.buildingRepository = buildingRepository;
        this.departmentService = departmentService;
    }

    public Page<Building> getAllBuildings(Pageable pageable) {
        return buildingRepository.findAll(pageable);
    }

    public Building getBuildingById(UUID id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Building with id " + id + " was not found."));
    }

    public Building createBuilding(Building building) {
        if (buildingRepository.existsByName(building.getName())) {
            throw new ConflictException("Building with this name already exists");
        }
        if (building.getAddress() != null && !building.getAddress().isBlank() && buildingRepository.existsByAddress(building.getAddress())) {
            throw new ConflictException("Building with this address already exists");
        }
        log.info("Created building {}", building.getName());
        return buildingRepository.save(building);

    }

    public Building patchSpecificBuilding(UUID id, Building newBuilding) {
        return buildingRepository.findById(id).map(building -> {
            Optional.ofNullable(newBuilding.getAddress()).ifPresent(addr -> {
                if (!addr.equals(building.getAddress()) && buildingRepository.existsByAddress(addr)) throw new ConflictException("Building with such address already exists.");
                building.setAddress(addr);
            });
            Optional.ofNullable(newBuilding.getName()).ifPresent(name -> {
                if (!name.equals(building.getName()) && buildingRepository.existsByName(name)) throw new ConflictException("Building with such name already exists");
                building.setName(name);
            });
            log.info("Updated building {}", building.getName());
            return buildingRepository.save(building);
        }).orElseThrow(() -> new NotFoundException("Building with id " + id + " was not found."));
    }

    @Transactional
    public void deleteBuilding(UUID id) {
        Building building = buildingRepository.findById(id).orElse(null);
        if (building != null) {
            List<UUID> departmentIds = building.getDepartments()
                    .stream()
                    .map(Department::getId)
                    .toList();
            departmentIds.forEach(departmentService::deleteDepartment);
        }
        log.info("Building {} - {} deleted", id, building != null ? building.getName() : "{UNDEFINED}");
        buildingRepository.deleteById(id);
    }
}
