package at.qe.skeleton.tests.services;

import at.qe.skeleton.model.Absence;
import at.qe.skeleton.repositories.AbsenceRepository;
import at.qe.skeleton.services.impl.AbsenceServiceImpl;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceUnitTests {

    @Mock
    private AbsenceRepository absenceRepository;

    @InjectMocks
    private AbsenceServiceImpl absenceService;

    private Absence sampleAbsence;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleAbsence = TestDataUtil.createAbsence(null);
    }

    @Test
    void testThatGetAllAbsencesByIdShouldReturnPageOfAbsences() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Absence> expectedPage = new PageImpl<>(List.of(sampleAbsence));

        when(absenceRepository.findAllByUserId(userId, pageable)).thenReturn(expectedPage);

        Page<Absence> result = absenceService.getAllAbsencesById(userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(sampleAbsence, result.getContent().get(0));

        verify(absenceRepository, times(1)).findAllByUserId(userId, pageable);
    }
}