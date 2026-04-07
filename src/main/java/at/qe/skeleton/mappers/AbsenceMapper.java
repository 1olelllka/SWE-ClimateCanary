package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.AbsenceDTO;
import at.qe.skeleton.model.Absence;
import org.springframework.stereotype.Component;

@Component
public class AbsenceMapper implements DTOMapper<Absence, AbsenceDTO> {
    @Override
    public AbsenceDTO mapTo(Absence entity) {
        return new AbsenceDTO(entity.getId(), entity.getTypeOfAbsence(), entity.getStatus(), entity.getStartDate(),
                entity.getEndDate(), entity.getCreatedAt(), entity.getAssignedTo(), entity.getComment());
    }

    @Override
    public Absence mapFrom(AbsenceDTO dto) {
        throw new UnsupportedOperationException("Function is not available.");
    }
}
