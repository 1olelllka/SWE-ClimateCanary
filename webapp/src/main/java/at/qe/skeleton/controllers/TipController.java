package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.TipCreateDTO;
import at.qe.skeleton.dtos.TipDTO;
import at.qe.skeleton.dtos.TipPatchDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.TipCreateMapper;
import at.qe.skeleton.mappers.TipMapper;
import at.qe.skeleton.model.Tip;
import at.qe.skeleton.services.TipService;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tips")
@RequiredArgsConstructor
@Tag(name = "Tip Management")
public class TipController {

    private final TipMapper mapper;
    private final TipCreateMapper tipCreateMapper;
    private final TipService tipService;

    @Operation(summary = "Create new tip. One of Permissions Required: CAN_MANAGE_TIPS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created new tip."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Tip for one of types/statuses/sensors conflict.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @PostMapping("")
    public ResponseEntity<TipDTO> createNewTip(@RequestBody @Valid TipCreateDTO dto,
                                               BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(""));
            throw new ValidationException(msg);
        }
        Tip tip = tipService.createTip(tipCreateMapper.mapFrom(dto));
        return new ResponseEntity<>(mapper.mapTo(tip), HttpStatus.CREATED);
    }

    @Operation(summary = "Get all tips. One of Permissions Required: CAN_MANAGE_TIPS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of tips."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @GetMapping("")
    public ResponseEntity<List<TipDTO>> getAllTips() {
        List<Tip> tips = tipService.getAllTips();
        return new ResponseEntity<>(tips.stream().map(mapper::mapTo).toList(), HttpStatus.OK);
    }

    @Operation(summary = "Update specific tip. One of Permissions Required: CAN_MANAGE_TIPS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated tip."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tip not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @PatchMapping("/{tip_id}")
    public ResponseEntity<TipDTO> patchMessageForTip(@PathVariable(name = "tip_id") UUID id,
                                                     @RequestBody @Valid TipPatchDTO dto,
                                                     BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(""));
            throw new ValidationException(msg);
        }
        Tip tip = tipService.updateExistingTip(id, dto.message());
        return new ResponseEntity<>(mapper.mapTo(tip), HttpStatus.OK);
    }

    @Operation(summary = "Delete specific tip. One of Permissions Required: CAN_MANAGE_TIPS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @DeleteMapping("/{tip_id}")
    public ResponseEntity<Void> deleteSpecificTip(@PathVariable(name = "tip_id")UUID id) {
        tipService.deleteTip(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
