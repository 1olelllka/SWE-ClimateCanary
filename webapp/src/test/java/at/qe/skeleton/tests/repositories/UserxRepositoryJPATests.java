package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.UserxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserxRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    UserxRepository userxRepository;

    private Department department;
    private Room room;
    private UserRole managerRole;
    private UserRole employeeRole;
    private Userx manager;
    private Userx employee;
    private Userx otherDeptUser;

    @BeforeEach
    void setUp() {
        Building b = em.persist(Building.builder().name("Building").build());
        department = Department.builder().build();
        department.setName("Engineering");
        department.setBuilding(b);
        em.persist(department);

        Department otherDept = Department.builder().build();
        otherDept.setName("HR");
        otherDept.setBuilding(b);
        em.persist(otherDept);

        room = Room.builder().roomNumber("321").roomType(RoomType.OFFICE).defaultPeopleCnt(10).isActive(true).department(department).build();
        em.persist(room);

        Room otherRoom = Room.builder().roomType(RoomType.OFFICE).isActive(true).roomNumber("123").defaultPeopleCnt(10).department(otherDept).build();
        em.persist(otherRoom);

        managerRole = new UserRole();
        managerRole.setName("DEPARTMENT_MANAGER");
        managerRole.setPermissions(Set.of(Permission.CAN_MANAGE_ABSENCES));
        em.persist(managerRole);

        employeeRole = new UserRole();
        employeeRole.setName("EMPLOYEE");
        employeeRole.setPermissions(Set.of());
        em.persist(employeeRole);

        manager = Userx.builder()
                .username("manager_user")
                .password("pass")
                .myRoom(room)
                .userRoles(Set.of(managerRole))
                .enabled(true)
                .build();
        em.persist(manager);

        employee = Userx.builder()
                .username("employee_user")
                .password("pass")
                .myRoom(room)
                .userRoles(Set.of(employeeRole))
                .enabled(true)
                .build();
        em.persist(employee);

        otherDeptUser = Userx.builder()
                .username("other_dept_user")
                .password("pass")
                .myRoom(otherRoom)
                .userRoles(Set.of(employeeRole))
                .enabled(true)
                .build();
        em.persist(otherDeptUser);

        em.flush();
    }

    // --- findFirstByUsername ---

    @Test
    void testFindFirstByUsername_found() {
        Optional<Userx> result = userxRepository.findFirstByUsername("manager_user");
        assertTrue(result.isPresent());
        assertEquals("manager_user", result.get().getUsername());
    }

    @Test
    void testFindFirstByUsername_notFound() {
        Optional<Userx> result = userxRepository.findFirstByUsername("ghost");
        assertTrue(result.isEmpty());
    }

    // --- existsByUsername ---

    @Test
    void testExistsByUsername_exists() {
        assertTrue(userxRepository.existsByUsername("manager_user"));
    }

    @Test
    void testExistsByUsername_doesNotExist() {
        assertFalse(userxRepository.existsByUsername("ghost"));
    }

    // --- findByRoleName ---

    @Test
    void testFindByRoleName_returnsUsersWithRole() {
        List<Userx> result = userxRepository.findByRoleName("DEPARTMENT_MANAGER");
        assertEquals(1, result.size());
        assertEquals("manager_user", result.get(0).getUsername());
    }

    @Test
    void testFindByRoleName_noMatch() {
        List<Userx> result = userxRepository.findByRoleName("NONEXISTENT");
        assertTrue(result.isEmpty());
    }

    // --- findByUsernameWithRoles ---

    @Test
    void testFindByUsernameWithRoles_fetchesRolesAndPermissions() {
        Optional<Userx> result = userxRepository.findByUsernameWithRoles("manager_user");
        assertTrue(result.isPresent());
        Userx u = result.get();
        assertFalse(u.getUserRoles().isEmpty());
        assertNotNull(u.getMyRoom());
        assertNotNull(u.getMyRoom().getDepartment());
    }

    @Test
    void testFindByUsernameWithRoles_notFound() {
        Optional<Userx> result = userxRepository.findByUsernameWithRoles("ghost");
        assertTrue(result.isEmpty());
    }

    // --- findAllByDepartment ---

    @Test
    void testFindAllByDepartment_returnsOnlyUsersInDepartment() {
        List<Userx> result = userxRepository.findAllByDepartment(department.getId());
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getUsername().equals("manager_user")));
        assertTrue(result.stream().anyMatch(u -> u.getUsername().equals("employee_user")));
    }

    @Test
    void testFindAllByDepartment_excludesOtherDepartments() {
        List<Userx> result = userxRepository.findAllByDepartment(department.getId());
        assertTrue(result.stream().noneMatch(u -> u.getUsername().equals("other_dept_user")));
    }
}