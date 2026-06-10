package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.Tip;
import at.qe.skeleton.repositories.TipRepository;
import at.qe.skeleton.repositories.WarningRepository;
import at.qe.skeleton.services.TipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipServiceImpl implements TipService {

    private final TipRepository tipRepository;
    private final WarningRepository warningRepository;

    @Override
    public Tip createTip(Tip tip) {
        if (tipRepository.existsByViolationStatusAndViolationTypeAndViolatedSensor(tip.getViolationStatus(), tip.getViolationType(), tip.getViolatedSensor())) {
            throw new ConflictException("Tip with such violation status already exists. In order to create new one delete this tip.");
        }
        return tipRepository.save(tip);
    }

    @Override
    public List<Tip> getAllTips() {
        return tipRepository.findAll();
    }

    @Override
    public Tip updateExistingTip(UUID id, String newMsg) {
        return tipRepository.findById(id).map(tip -> {
            tip.setMsg(newMsg);
            log.info("Successfully updated message for tip - {}, {}, {}", tip.getViolatedSensor().name(), tip.getViolationType().name(), tip.getViolationStatus().name());
            return tipRepository.save(tip);
        })
        .orElseThrow(() -> new NotFoundException("Tip with id " + id + " was not found."));
    }

    @Override
    public void deleteTip(UUID id) {
        Optional<Tip> optionalTip = tipRepository.findById(id);
        if (optionalTip.isPresent()) {
            optionalTip.get().getWarnings().stream().forEach(warning -> {
                warning.setTip(null);
                warningRepository.save(warning);
            });
            log.info("Deleted tip - {} {} {}", optionalTip.get().getViolatedSensor().name(), optionalTip.get().getViolationType().name(), optionalTip.get().getViolationStatus().name());
            tipRepository.deleteById(id);
        }
    }


}
