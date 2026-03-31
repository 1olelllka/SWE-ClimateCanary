package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class DepartmentRepositoryDataJPATests {

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private BuildingRepository buildingRepository;

    private Department createdDepartment;
    @BeforeEach
    void setUp() {
        Building building = buildingRepository.save(TestDataUtil.createBuildingEntity());
        this.createdDepartment = departmentRepository.save(TestDataUtil.createDepartmentEntity(building));
    }

    @Test
    public void testThatExistsByNameWorksAsExpected() {
        assertAll(
                () -> assertTrue(departmentRepository.existsByNameAndBuildingId(createdDepartment.getName(), createdDepartment.getBuilding().getId())),
                () -> assertFalse(departmentRepository.existsByNameAndBuildingId(UUID.randomUUID().toString(), createdDepartment.getBuilding().getId()))
        );
    }
}
