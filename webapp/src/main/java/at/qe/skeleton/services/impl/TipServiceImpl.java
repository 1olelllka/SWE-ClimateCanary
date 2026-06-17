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

/**
 * Implementation of {@link TipService} managing {@link Tip} entities. A tip
 * provides advisory text associated with a specific combination of violation
 * status, violation type, and violated sensor. The combination must be unique —
 * at most one tip exists per triplet.
 *
 * <p>When a tip is deleted, all warnings referencing it have their tip reference
 * cleared before the tip itself is removed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TipServiceImpl implements TipService {

    private final TipRepository tipRepository;
    private final WarningRepository warningRepository;

    /**
     * Creates and persists a new tip. The combination of violation status, violation
     * type, and violated sensor must be unique.
     *
     * @param tip the tip to create
     * @return the saved {@link Tip}
     * @throws ConflictException if a tip with the same violation triplet already exists
     */
    @Override
    public Tip createTip(Tip tip) {
        if (tipRepository.existsByViolationStatusAndViolationTypeAndViolatedSensor(tip.getViolationStatus(), tip.getViolationType(), tip.getViolatedSensor())) {
            throw new ConflictException("Tip with such violation status already exists. In order to create new one delete this tip.");
        }
        return tipRepository.save(tip);
    }

    /**
     * Returns all existing tips.
     *
     * @return list of all {@link Tip} entities
     */
    @Override
    public List<Tip> getAllTips() {
        return tipRepository.findAll();
    }

    /**
     * Updates the advisory message of an existing tip.
     *
     * @param id     the UUID of the tip to update
     * @param newMsg the new message text
     * @return the updated {@link Tip}
     * @throws NotFoundException if no tip with that ID exists
     */
    @Override
    public Tip updateExistingTip(UUID id, String newMsg) {
        return tipRepository.findById(id).map(tip -> {
            tip.setMsg(newMsg);
            log.info("Successfully updated message for tip - {}, {}, {}", tip.getViolatedSensor().name(), tip.getViolationType().name(), tip.getViolationStatus().name());
            return tipRepository.save(tip);
        })
        .orElseThrow(() -> new NotFoundException("Tip with id " + id + " was not found."));
    }

    /**
     * Deletes the tip with the given ID. Before removal, all warnings that reference
     * the tip have their tip field set to {@code null} to avoid orphaned references.
     * If no tip with that ID exists, this method is a no-op.
     *
     * @param id the UUID of the tip to delete
     */
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