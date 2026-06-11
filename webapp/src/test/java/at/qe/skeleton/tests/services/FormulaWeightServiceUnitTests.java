package at.qe.skeleton.tests.services;

import at.qe.skeleton.dtos.FormulaWeightCreateDTO;
import at.qe.skeleton.dtos.FormulaWeightDTO;
import at.qe.skeleton.model.FormulaWeights;
import at.qe.skeleton.repositories.FormulaWeightsRepository;
import at.qe.skeleton.services.impl.FormulaWeightServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormulaWeightServiceUnitTests {

    @Mock
    private FormulaWeightsRepository repository;

    @InjectMocks
    private FormulaWeightServiceImpl service;

    private FormulaWeights existingWeight;

    @BeforeEach
    void setUp() {
        existingWeight = FormulaWeights.builder()
                .tempWeight(0.4)
                .humWeight(0.3)
                .co2Weight(0.3)
                .modifiedAt(LocalDateTime.of(2024, 1, 1, 12, 0))
                .build();
    }

    // -------------------------------------------------------------------------
    // getFormulaWeight()
    // -------------------------------------------------------------------------

    @Test
    void getFormulaWeight_whenNoWeightsExist_savesAndReturnsDefaults() {
        // Arrange
        FormulaWeights saved = FormulaWeights.builder()
                .tempWeight(0.4)
                .humWeight(0.3)
                .co2Weight(0.3)
                .modifiedAt(LocalDateTime.now())
                .build();
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(repository.save(any(FormulaWeights.class))).thenReturn(saved);

        // Act
        FormulaWeightDTO result = service.getFormulaWeight();

        // Assert
        assertThat(result.tempWeight()).isEqualTo(0.4);
        assertThat(result.humWeight()).isEqualTo(0.3);
        assertThat(result.co2Weight()).isEqualTo(0.3);
        assertThat(result.modifiedAt()).isNotNull();
        verify(repository, times(1)).save(any(FormulaWeights.class));
    }

    @Test
    void getFormulaWeight_whenNoWeightsExist_savesEntityWithCorrectDefaultValues() {
        // Arrange
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(repository.save(any(FormulaWeights.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.getFormulaWeight();

        // Assert — verify the exact values persisted
        ArgumentCaptor<FormulaWeights> captor = ArgumentCaptor.forClass(FormulaWeights.class);
        verify(repository).save(captor.capture());
        FormulaWeights persisted = captor.getValue();
        assertThat(persisted.getTempWeight()).isEqualTo(0.4);
        assertThat(persisted.getHumWeight()).isEqualTo(0.3);
        assertThat(persisted.getCo2Weight()).isEqualTo(0.3);
        assertThat(persisted.getModifiedAt()).isNotNull();
    }

    @Test
    void getFormulaWeight_whenWeightsExist_returnsFirstRowWithoutSaving() {
        // Arrange
        when(repository.findAll()).thenReturn(List.of(existingWeight));

        // Act
        FormulaWeightDTO result = service.getFormulaWeight();

        // Assert
        assertThat(result.tempWeight()).isEqualTo(existingWeight.getTempWeight());
        assertThat(result.humWeight()).isEqualTo(existingWeight.getHumWeight());
        assertThat(result.co2Weight()).isEqualTo(existingWeight.getCo2Weight());
        assertThat(result.modifiedAt()).isEqualTo(existingWeight.getModifiedAt());
        verify(repository, never()).save(any());
    }

    @Test
    void getFormulaWeight_whenMultipleWeightsExist_returnsFirstRow() {
        // Arrange
        FormulaWeights second = FormulaWeights.builder()
                .tempWeight(0.5)
                .humWeight(0.25)
                .co2Weight(0.25)
                .modifiedAt(LocalDateTime.now())
                .build();
        when(repository.findAll()).thenReturn(List.of(existingWeight, second));

        // Act
        FormulaWeightDTO result = service.getFormulaWeight();

        // Assert — only the first element must be mapped
        assertThat(result.tempWeight()).isEqualTo(existingWeight.getTempWeight());
        assertThat(result.humWeight()).isEqualTo(existingWeight.getHumWeight());
        assertThat(result.co2Weight()).isEqualTo(existingWeight.getCo2Weight());
    }

    // -------------------------------------------------------------------------
    // patchFormulaWeights()
    // -------------------------------------------------------------------------

    @Test
    void patchFormulaWeights_whenNoWeightsExist_savesNewEntityFromDTO() {
        // Arrange
        FormulaWeightCreateDTO dto = new FormulaWeightCreateDTO(0.5, 0.25, 0.25);
        FormulaWeights saved = FormulaWeights.builder()
                .tempWeight(dto.tempWeight())
                .humWeight(dto.humWeight())
                .co2Weight(dto.co2Weight())
                .modifiedAt(LocalDateTime.now())
                .build();
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(repository.save(any(FormulaWeights.class))).thenReturn(saved);

        // Act
        FormulaWeightDTO result = service.patchFormulaWeights(dto);

        // Assert
        assertThat(result.tempWeight()).isEqualTo(0.5);
        assertThat(result.humWeight()).isEqualTo(0.25);
        assertThat(result.co2Weight()).isEqualTo(0.25);
        assertThat(result.modifiedAt()).isNotNull();
        verify(repository, times(1)).save(any(FormulaWeights.class));
    }

    @Test
    void patchFormulaWeights_whenNoWeightsExist_persistsCorrectDTOValues() {
        // Arrange
        FormulaWeightCreateDTO dto = new FormulaWeightCreateDTO(0.6, 0.2, 0.2);
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(repository.save(any(FormulaWeights.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.patchFormulaWeights(dto);

        // Assert
        ArgumentCaptor<FormulaWeights> captor = ArgumentCaptor.forClass(FormulaWeights.class);
        verify(repository).save(captor.capture());
        FormulaWeights persisted = captor.getValue();
        assertThat(persisted.getTempWeight()).isEqualTo(0.6);
        assertThat(persisted.getHumWeight()).isEqualTo(0.2);
        assertThat(persisted.getCo2Weight()).isEqualTo(0.2);
        assertThat(persisted.getModifiedAt()).isNotNull();
    }

    @Test
    void patchFormulaWeights_whenWeightsExist_updatesFirstRowAndSaves() {
        // Arrange
        FormulaWeightCreateDTO dto = new FormulaWeightCreateDTO(0.5, 0.1, 0.4);
        when(repository.findAll()).thenReturn(List.of(existingWeight));
        when(repository.save(any(FormulaWeights.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        FormulaWeightDTO result = service.patchFormulaWeights(dto);

        // Assert
        assertThat(result.tempWeight()).isEqualTo(0.5);
        assertThat(result.humWeight()).isEqualTo(0.4);
        assertThat(result.co2Weight()).isEqualTo(0.1);
        verify(repository, times(1)).save(existingWeight);
    }

    @Test
    void patchFormulaWeights_whenWeightsExist_updatesModifiedAt() {
        // Arrange
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        FormulaWeightCreateDTO dto = new FormulaWeightCreateDTO(0.5, 0.1, 0.4);
        when(repository.findAll()).thenReturn(List.of(existingWeight));
        when(repository.save(any(FormulaWeights.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        FormulaWeightDTO result = service.patchFormulaWeights(dto);

        // Assert — modifiedAt must be refreshed to now, not the original 2024-01-01 value
        assertThat(result.modifiedAt()).isAfter(before);
        assertThat(result.modifiedAt()).isNotEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0));
    }

    @Test
    void patchFormulaWeights_whenWeightsExist_doesNotCreateNewRow() {
        // Arrange
        FormulaWeightCreateDTO dto = new FormulaWeightCreateDTO(0.5, 0.1, 0.4);
        when(repository.findAll()).thenReturn(List.of(existingWeight));
        when(repository.save(any(FormulaWeights.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.patchFormulaWeights(dto);

        // Assert — save must be called exactly once with the existing entity, not a new one
        ArgumentCaptor<FormulaWeights> captor = ArgumentCaptor.forClass(FormulaWeights.class);
        verify(repository, times(1)).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existingWeight);
    }

    @Test
    void patchFormulaWeights_whenMultipleWeightsExist_onlyUpdatesFirstRow() {
        // Arrange
        FormulaWeights second = FormulaWeights.builder()
                .tempWeight(0.5)
                .humWeight(0.25)
                .co2Weight(0.25)
                .modifiedAt(LocalDateTime.now())
                .build();
        FormulaWeightCreateDTO dto = new FormulaWeightCreateDTO(0.7, 0.15, 0.15);
        when(repository.findAll()).thenReturn(List.of(existingWeight, second));
        when(repository.save(any(FormulaWeights.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.patchFormulaWeights(dto);

        // Assert — only the first entity must be passed to save
        ArgumentCaptor<FormulaWeights> captor = ArgumentCaptor.forClass(FormulaWeights.class);
        verify(repository, times(1)).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existingWeight);
        assertThat(second.getTempWeight()).isEqualTo(0.5); // untouched
    }
}