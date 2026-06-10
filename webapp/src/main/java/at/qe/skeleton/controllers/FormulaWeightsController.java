package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.FormulaWeightCreateDTO;
import at.qe.skeleton.dtos.FormulaWeightDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.services.FormulaWeightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FormulaWeightsController {

    private final FormulaWeightService service;

    @GetMapping("/weights")
    ResponseEntity<FormulaWeightDTO> getCurrentFormulaWeights() {
        return new ResponseEntity<>(service.getFormulaWeight(), HttpStatus.OK);
    }

    @PatchMapping("/weights")
    ResponseEntity<FormulaWeightDTO> patchCurrentFormulaWeights(@RequestBody @Valid  FormulaWeightCreateDTO dto,
                                                                BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        if (dto.tempWeight() + dto.co2Weight() + dto.humWeight() > 1) throw new ValidationException("Invalid values: Sum of all weights must not be more than 1.");
        if (dto.tempWeight() + dto.co2Weight() + dto.humWeight() < 1) throw new ValidationException("Invalid values: Sum of all weights must not be less than 1.");
        return new ResponseEntity<>(service.patchFormulaWeights(dto), HttpStatus.OK);
    }
}
