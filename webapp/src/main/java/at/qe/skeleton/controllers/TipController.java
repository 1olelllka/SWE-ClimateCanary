package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.TipCreateDTO;
import at.qe.skeleton.dtos.TipDTO;
import at.qe.skeleton.dtos.TipPatchDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.TipCreateMapper;
import at.qe.skeleton.mappers.TipMapper;
import at.qe.skeleton.model.Tip;
import at.qe.skeleton.services.TipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tips")
@RequiredArgsConstructor
public class TipController {

    private final TipMapper mapper;
    private final TipCreateMapper tipCreateMapper;
    private final TipService tipService;

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

    @GetMapping("")
    public ResponseEntity<List<TipDTO>> getAllTips() {
        List<Tip> tips = tipService.getAllTips();
        return new ResponseEntity<>(tips.stream().map(mapper::mapTo).toList(), HttpStatus.OK);
    }

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

    @DeleteMapping("/{tip_id}")
    public ResponseEntity<Void> deleteSpecificTip(@PathVariable(name = "tip_id")UUID id) {
        tipService.deleteTip(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
