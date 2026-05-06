package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.Building;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class BuildingRepositoryDataJPATests {

    @Autowired
    private BuildingRepository buildingRepository;

    private Building createdBuilding;
    @BeforeEach
    void setUp() {
        this.createdBuilding = buildingRepository.save(TestDataUtil.createBuildingEntity());
    }

    @Test
    public void testThatExistsByNameWorksAsExpected() {
        assertAll(
                () -> assertTrue(buildingRepository.existsByName(createdBuilding.getName())),
                () -> assertFalse(buildingRepository.existsByName(UUID.randomUUID().toString()))
        );
    }

    @Test
    public void testThatExistsByAddressWorksAsExpected() {
        assertAll(
                () -> assertTrue(buildingRepository.existsByAddress(createdBuilding.getAddress())),
                () -> assertFalse(buildingRepository.existsByAddress(UUID.randomUUID().toString()))
        );
    }

}
