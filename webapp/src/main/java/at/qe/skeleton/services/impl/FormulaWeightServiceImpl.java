package at.qe.skeleton.services.impl;

import at.qe.skeleton.dtos.FormulaWeightCreateDTO;
import at.qe.skeleton.dtos.FormulaWeightDTO;
import at.qe.skeleton.model.FormulaWeights;
import at.qe.skeleton.repositories.FormulaWeightsRepository;
import at.qe.skeleton.services.FormulaWeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of {@link FormulaWeightService} managing the single
 * {@link FormulaWeights} record that controls the temperature, humidity, and CO₂
 * weighting used by {@link at.qe.skeleton.background.TrendJob#avgFormula}.
 *
 * <p>The table is treated as a singleton: at most one row exists. If the table is
 * empty when a read or write is requested, a default record
 * ({@code temp=0.4, hum=0.3, co2=0.3}) is created automatically.
 */
@Service
@RequiredArgsConstructor
public class FormulaWeightServiceImpl implements FormulaWeightService {

    private final FormulaWeightsRepository repository;

    /**
     * Returns the current formula weights. If no record exists yet, a default record
     * ({@code temp=0.4, hum=0.3, co2=0.3}) is persisted and returned.
     *
     * @return a {@link FormulaWeightDTO} reflecting the current weights and last-modified timestamp
     */
    @Override
    public FormulaWeightDTO getFormulaWeight() {
        List<FormulaWeights> weights = repository.findAll();
        if (weights.isEmpty()) {
            FormulaWeights newWeight = repository.save(FormulaWeights.builder().tempWeight(0.4).humWeight(0.3).co2Weight(0.3).modifiedAt(LocalDateTime.now()).build());
            return new FormulaWeightDTO(newWeight.getTempWeight(), newWeight.getCo2Weight(), newWeight.getHumWeight(), newWeight.getModifiedAt());
        } else {
            FormulaWeights weight = weights.getFirst();
            return new FormulaWeightDTO(weight.getTempWeight(), weight.getCo2Weight(), weight.getHumWeight(), weight.getModifiedAt());
        }
    }

    /**
     * Updates the formula weights with the values from the given DTO. If no record
     * exists yet, a new one is created; otherwise the existing record is overwritten.
     * The {@code modifiedAt} timestamp is always set to the current time.
     *
     * @param dto the new weight values to apply
     * @return a {@link FormulaWeightDTO} reflecting the updated weights and timestamp
     */
    @Override
    public FormulaWeightDTO patchFormulaWeights(FormulaWeightCreateDTO dto) {
        List<FormulaWeights> weights = repository.findAll();
        if (weights.isEmpty()) {
            FormulaWeights newWeight = repository.save(FormulaWeights.builder()
                                                        .tempWeight(dto.tempWeight())
                                                        .humWeight(dto.humWeight())
                                                        .co2Weight(dto.co2Weight())
                                                        .modifiedAt(LocalDateTime.now())
                                                        .build());
            return new FormulaWeightDTO(newWeight.getTempWeight(), newWeight.getCo2Weight(), newWeight.getHumWeight(), newWeight.getModifiedAt());
        } else {
            FormulaWeights weight = weights.getFirst();
            weight.setCo2Weight(dto.co2Weight());
            weight.setHumWeight(dto.humWeight());
            weight.setTempWeight(dto.tempWeight());
            weight.setModifiedAt(LocalDateTime.now());
            weight = repository.save(weight);
            return new FormulaWeightDTO(weight.getTempWeight(), weight.getCo2Weight(), weight.getHumWeight(), weight.getModifiedAt());
        }
    }
}