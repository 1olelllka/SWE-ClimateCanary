package at.qe.skeleton.services;

import at.qe.skeleton.model.Building;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BuildingService {
    Page<Building> getAllBuildings(Pageable pageable);
    Building createBuilding(Building building);
    Building patchSpecificBuilding(UUID id, Building newBuilding);
    void deleteBuilding(UUID id);
    Building getBuildingById(UUID id);
}
