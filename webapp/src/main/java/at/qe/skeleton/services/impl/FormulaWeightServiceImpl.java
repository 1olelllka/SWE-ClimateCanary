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

@Service
@RequiredArgsConstructor
public class FormulaWeightServiceImpl implements FormulaWeightService {

    private final FormulaWeightsRepository repository;

    @Override
    public FormulaWeightDTO getFormulaWeight() {
        List<FormulaWeights> weights = repository.findAll();
        if (weights.isEmpty()) {
            FormulaWeights newWeight = repository.save(FormulaWeights.builder().modifiedAt(LocalDateTime.now()).build());
            return new FormulaWeightDTO(newWeight.getTempWeight(), newWeight.getCo2Weight(), newWeight.getHumWeight(), newWeight.getModifiedAt());
        } else {
            FormulaWeights weight = weights.getFirst();
            return new FormulaWeightDTO(weight.getTempWeight(), weight.getCo2Weight(), weight.getHumWeight(), weight.getModifiedAt());
        }
    }

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
