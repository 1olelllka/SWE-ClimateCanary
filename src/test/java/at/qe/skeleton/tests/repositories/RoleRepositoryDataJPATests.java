package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.Permission;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.repositories.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
public class RoleRepositoryDataJPATests {

    private RoleRepository roleRepository;

    @Autowired
    public RoleRepositoryDataJPATests(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @BeforeEach
    void setUp() {
        UserRole userRole = UserRole.builder().name("TEST_ROLE").permissions(Set.of(Permission.CAN_MANAGE_OWN_ABSENCE)).build();
        roleRepository.save(userRole);
    }

    @Test
    public void testThatFindByNameReturnsCorrectResults() {
        assertTrue(roleRepository.findByName("TEST_ROLE").isPresent());
        assertTrue(roleRepository.findByName("INCORRECT_ROLE").isEmpty());
    }

}
