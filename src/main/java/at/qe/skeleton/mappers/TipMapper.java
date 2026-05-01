package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.TipDTO;
import at.qe.skeleton.model.Tip;
import org.springframework.stereotype.Component;

@Component
public class TipMapper implements DTOMapper<Tip, TipDTO> {

    @Override
    public TipDTO mapTo(Tip entity) {
        return new TipDTO(
                entity.getId(),
                entity.getViolationStatus(),
                entity.getViolationType(),
                entity.getViolatedSensor(),
                entity.getMsg()
        );
    }

    @Override
    public Tip mapFrom(TipDTO dto) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
