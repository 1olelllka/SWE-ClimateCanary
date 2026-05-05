package at.qe.skeleton.services;

import at.qe.skeleton.model.Tip;

import java.util.List;
import java.util.UUID;

public interface TipService {
    Tip createTip(Tip tip);

    List<Tip> getAllTips();

    Tip updateExistingTip(UUID id, String newMsg);

    void deleteTip(UUID id);
}
