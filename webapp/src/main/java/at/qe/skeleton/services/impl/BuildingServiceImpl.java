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

/**
 * Implementation of {@link BuildingService} providing CRUD operations for
 * {@link Building} entities. Cascades deletion to all departments (and their
 * contents) via {@link DepartmentService}.
 */
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

    /**
     * Returns a paginated list of all buildings.
     *
     * @param pageable pagination parameters
     * @return page of {@link Building} entities
     */
    public Page<Building> getAllBuildings(Pageable pageable) {
        return buildingRepository.findAll(pageable);
    }

    /**
     * Returns the building with the given ID.
     *
     * @param id the building UUID
     * @return the matching {@link Building}
     * @throws NotFoundException if no building with that ID exists
     */
    public Building getBuildingById(UUID id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Building with id " + id + " was not found."));
    }

    /**
     * Creates and persists a new building. Name and address (if provided) must be
     * unique across all existing buildings.
     *
     * @param building the building to create
     * @return the saved {@link Building}
     * @throws ConflictException if a building with the same name or address already exists
     */
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

    /**
     * Applies a partial update to the building with the given ID. Only non-null
     * fields in {@code newBuilding} are applied. Uniqueness of name and address is
     * re-validated against other buildings (the building's own current values are
     * excluded from the conflict check).
     *
     * @param id          the UUID of the building to update
     * @param newBuilding a partial {@link Building} carrying the fields to update
     * @return the updated {@link Building}
     * @throws NotFoundException if no building with that ID exists
     * @throws ConflictException if the new name or address is already taken by another building
     */
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

    /**
     * Deletes the building with the given ID along with all of its departments.
     * Each department is removed via {@link DepartmentService#deleteDepartment} to
     * ensure cascading cleanup. If no building with that ID exists, the delete is
     * a no-op.
     *
     * @param id the UUID of the building to delete
     */
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