package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.FormulaWeightCreateDTO;
import at.qe.skeleton.dtos.FormulaWeightDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.services.FormulaWeightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Formula Weights Configuration")
public class FormulaWeightsController {

    private final FormulaWeightService service;

    @Operation(summary = "Get current formula weights. CAN_MANAGE_WEIGHT_FORMULA permission required")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current formula."),
            @ApiResponse(responseCode = "403", description = "Unauthorized.", content = @Content())
    })
    @GetMapping("/weights")
    ResponseEntity<FormulaWeightDTO> getCurrentFormulaWeights() {
        return new ResponseEntity<>(service.getFormulaWeight(), HttpStatus.OK);
    }

    @Operation(summary = "Update current formula weights. CAN_MANAGE_WEIGHT_FORMULA permission required")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated formula."),
            @ApiResponse(responseCode = "400", description = "Validation error.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Unauthorized.", content = @Content())
    })
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
