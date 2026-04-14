package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.model.Absence;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.AbsenceRepository;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class AbsenceRepositoryDataJPATests {

    @Autowired
    private AbsenceRepository absenceRepository;
    @Autowired
    private UserxRepository userxRepository;

    private Absence createdAbsence;
    private UUID userId;
    @BeforeEach
    void setUp() {
        Userx user = userxRepository.save(TestDataUtil.createUserxEntity(null, null));
        userId = user.getId();
        createdAbsence = TestDataUtil.createAbsence(user);
        absenceRepository.save(createdAbsence);
    }

    @Test
    void testThatFindAllByUserIdReturnsPageOfResults() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Absence> result = absenceRepository.findAllByUserId(userId, pageable);
        Page<Absence> invalid = absenceRepository.findAllByUserId(UUID.randomUUID(), pageable);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(1, result.getTotalElements()),
                () -> assertEquals(this.userId, result.getContent().getFirst().getUser().getId())
        );

        assertAll(
                () -> assertNotNull(invalid),
                () -> assertEquals(0, invalid.getTotalElements())
        );
    }

}
