package at.qe.skeleton.tests;

import at.qe.skeleton.model.Building;

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
}
