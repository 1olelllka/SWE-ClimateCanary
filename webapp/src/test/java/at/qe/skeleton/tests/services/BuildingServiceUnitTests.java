package at.qe.skeleton.tests.services;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.services.impl.BuildingServiceImpl;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingServiceUnitTests {

    @Mock
    private BuildingRepository buildingRepository;
    @InjectMocks
    private BuildingServiceImpl buildingService;

    private Building sampleBuilding;
    private UUID buildingId;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
        sampleBuilding = TestDataUtil.createBuildingEntity();
        sampleBuilding.setId(buildingId);
        sampleBuilding.setName("Technik Campus");
        sampleBuilding.setAddress("Technikerstraße 13");
    }

    @Test
    void testThatGetAllBuildingsShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Building> page = new PageImpl<>(List.of(sampleBuilding));
        when(buildingRepository.findAll(pageable)).thenReturn(page);

        Page<Building> result = buildingService.getAllBuildings(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(buildingRepository).findAll(pageable);
    }


    @Test
    void testThatGetBuildingByIdWhenExistsShouldReturnBuilding() {
        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(sampleBuilding));

        Building result = buildingService.getBuildingById(buildingId);

        assertEquals(sampleBuilding.getName(), result.getName());
    }

    @Test
    void testThatGetBuildingByIdWhenNotExistsShouldThrowNotFoundException() {
        when(buildingRepository.findById(buildingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> buildingService.getBuildingById(buildingId));
    }


    @Test
    void testThatCreateBuildingSuccessful() {
        when(buildingRepository.existsByName(any())).thenReturn(false);
        when(buildingRepository.existsByAddress(any())).thenReturn(false);
        when(buildingRepository.save(any())).thenReturn(sampleBuilding);

        Building result = buildingService.createBuilding(sampleBuilding);

        assertNotNull(result);
        verify(buildingRepository).save(sampleBuilding);
    }

    @Test
    void testThatCreateBuildingShouldThrowConflictIfNameExists() {
        when(buildingRepository.existsByName(sampleBuilding.getName())).thenReturn(true);

        assertThrows(ConflictException.class, () -> buildingService.createBuilding(sampleBuilding));
        verify(buildingRepository, never()).save(any());
    }

    @Test
    void testThatPatchSpecificBuildingUpdatesName() {
        Building patchData = new Building();
        patchData.setName("New Name");

        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(sampleBuilding));
        when(buildingRepository.existsByName("New Name")).thenReturn(false);
        when(buildingRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Building result = buildingService.patchSpecificBuilding(buildingId, patchData);

        assertEquals("New Name", result.getName());
        assertEquals("Technikerstraße 13", result.getAddress()); // Address should remain unchanged
    }

    @Test
    void testThatPatchSpecificBuildingShouldThrowConflict() {
        Building patchData = new Building();
        patchData.setName("Existing Name");

        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(sampleBuilding));
        when(buildingRepository.existsByName("Existing Name")).thenReturn(true);

        assertThrows(ConflictException.class, () -> buildingService.patchSpecificBuilding(buildingId, patchData));
    }

    @Test
    void testThatPatchSpecificBuildingShouldThrowNotFound() {
        when(buildingRepository.findById(buildingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> buildingService.patchSpecificBuilding(buildingId, new Building()));
    }

    @Test
    void testThatDeleteBuildingShouldCallRepository() {
        buildingService.deleteBuilding(buildingId);
        verify(buildingRepository, times(1)).deleteById(buildingId);
    }

}