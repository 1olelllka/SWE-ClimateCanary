package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.TipCreateDTO;
import at.qe.skeleton.model.Tip;
import org.springframework.stereotype.Component;

@Component
public class TipCreateMapper implements DTOMapper<Tip, TipCreateDTO> {

    @Override
    public TipCreateDTO mapTo(Tip entity) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Tip mapFrom(TipCreateDTO dto) {
        Tip tip = new Tip();
        tip.setMsg(dto.message());
        tip.setViolationType(dto.violationType());
        tip.setViolatedSensor(dto.violatedSensor());
        // warning will be resolved in service via dto.roomID()
        return tip;
    }
}