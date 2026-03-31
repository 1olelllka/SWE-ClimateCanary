package at.qe.skeleton.tests;

import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;

import java.util.ArrayList;

public class TestDataUtil {

    public static Building createBuildingEntity() {
        return Building
                .builder()
                .name("Test Building")
                .address("Test Address")
                .departments(new ArrayList<>())
                .build();
    }

    public static Department createDepartmentEntity(Building building) {
        return Department
                .builder()
                .name("Test Department")
                .building(building)
                .rooms(new ArrayList<>())
                .build();
    }
}
