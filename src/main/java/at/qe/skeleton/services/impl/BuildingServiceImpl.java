package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ResourceNotFoundException;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.services.BuildingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BuildingServiceImpl implements BuildingService {

    private final BuildingRepository buildingRepository;

    public BuildingServiceImpl(BuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
    }

    public Page<Building> getAllBuildings(Pageable pageable) {
        return buildingRepository.findAll(pageable);
    }

    public Building getBuildingById(UUID id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + id));
    }

//    public Building getBuildingByName(String name){
//        return buildingRepository.findByName(name)
//                .orElseThrow(() -> new ResourceNotFoundException("Building not found with name: " + name));
//    }
//
//    public Building getBuildingByAddress(String address){
//        return buildingRepository.findByName(address)
//                .orElseThrow(() -> new ResourceNotFoundException("Building not found with address: " + address));
//    }

    public Building createBuilding(Building building) {
        if (buildingRepository.existsByNameOrAddress(building.getName(), building.getAddress())) {
            throw new ConflictException("Building with this name or address already exists");
        }
        return buildingRepository.save(building);

    }

    public Building patchSpecificBuilding(UUID id, Building newBuilding) {
        return buildingRepository.findById(id).map(building -> {
            Optional.ofNullable(newBuilding.getAddress()).ifPresent(building::setAddress);
            Optional.ofNullable(newBuilding.getName()).ifPresent(building::setName);
            return buildingRepository.save(building);
        }).orElseThrow(() -> new NotFoundException("Building with id " + id + " was not found."));
    }

    public void deleteBuilding(UUID id) {
        buildingRepository.deleteById(id);
    }

    public List<Department> getDepartmentsByBuilding(UUID buildingId) {
        Building building = getBuildingById(buildingId); // reuse your existing method!
        return building.getDepartments();
    }
}
