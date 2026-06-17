package at.qe.skeleton.services;

import at.qe.skeleton.dtos.FormulaWeightCreateDTO;
import at.qe.skeleton.dtos.FormulaWeightDTO;

public interface FormulaWeightService {
    FormulaWeightDTO getFormulaWeight();

    FormulaWeightDTO patchFormulaWeights(FormulaWeightCreateDTO dto);
}
