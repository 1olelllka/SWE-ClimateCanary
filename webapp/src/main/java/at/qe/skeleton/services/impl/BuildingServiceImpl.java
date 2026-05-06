package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.services.BuildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class BuildingServiceImpl implements BuildingService {

    private final BuildingRepository buildingRepository;

    @Autowired
    public BuildingServiceImpl(BuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
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
            return buildingRepository.save(building);
        }).orElseThrow(() -> new NotFoundException("Building with id " + id + " was not found."));
    }

    public void deleteBuilding(UUID id) {
        buildingRepository.deleteById(id);
    }
}
