package at.qe.skeleton.services;

import at.qe.skeleton.exceptions.ResourceNotFoundException;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.repositories.BuildingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;

    public BuildingService(BuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
    }

    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    public Building getBuildingById(UUID id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + id));
    }

    public Building getBuildingByName(String name){
        return buildingRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with name: " + name));
    }

    public Building getBuildingByAddress(String address){
        return buildingRepository.findByName(address)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with address: " + address));
    }

    public Building createBuilding(Building building) {

        if (buildingRepository.findByName(building.getName()).isPresent()) {
            throw new IllegalArgumentException("Building with this name already exists");
        }

        if (building.getAddress() != null &&
                buildingRepository.findByAddress(building.getAddress()).isPresent()) {
            throw new IllegalArgumentException("Building with this address already exists");
        }

        return buildingRepository.save(building);
    }

    public void deleteBuilding(UUID id) {
        buildingRepository.deleteById(id);
    }

    public List<Department> getDepartmentsByBuilding(UUID buildingId) {
        Building building = getBuildingById(buildingId); // reuse your existing method!
        return building.getDepartments();
    }
}
