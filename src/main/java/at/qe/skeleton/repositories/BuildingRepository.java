package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuildingRepository extends JpaRepository<Building, UUID> {
    Optional<Building> findByName(String name);
    Optional<Building> findByAddress(String address);
    boolean existsByNameOrAddress(String name, String address);
}
