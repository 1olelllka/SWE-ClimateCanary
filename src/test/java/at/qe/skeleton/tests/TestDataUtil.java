package at.qe.skeleton.tests;

import at.qe.skeleton.dtos.UserxCreateDTO;
import at.qe.skeleton.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

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

    public static Room createRoomEntity(Department department) {
        return Room.builder()
                .isActive(true)
                .roomType(RoomType.OFFICE)
                .department(department)
                .defaultPeopleCnt(10)
                .build();
    }

    public static UserRole createUserRole(Set<Permission> permissions) {
        return UserRole
                .builder()
                .name("TEST ROLE")
                .permissions(permissions)
                .build();
    }

    public static Userx createUserxEntity(UserRole userRole, Room room) {
        return Userx.builder()
                .username("jdoe")
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .userRoles(userRole != null  ? Set.of(userRole) : null)
                .myRoom(room)
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();
    }

    public static UserxCreateDTO createUserxCreateDTO(Set<UUID> roles) {
        return new UserxCreateDTO(
                "jdoe",
                "password",
                "John",
                "Doe",
                true,
                roles
        );
    }
}
