package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.AbsenceCreateDTO;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.Absence;
import at.qe.skeleton.repositories.UserxRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AbsenceCreateMapper implements DTOMapper<Absence, AbsenceCreateDTO> {

    private UserxRepository userxRepository;

    @Autowired
    public AbsenceCreateMapper(UserxRepository userxRepository) {
        this.userxRepository = userxRepository;
    }

    @Override
    public AbsenceCreateDTO mapTo(Absence entity) {
        throw new UnsupportedOperationException("Not available");
    }

    @Override
    public Absence mapFrom(AbsenceCreateDTO dto) {
        try {
            return Absence
                    .builder()
                    .user(userxRepository.getReferenceById(dto.userId()))
                    .comment(dto.comment())
                    .startDate(dto.startDate())
                    .endDate(dto.endDate())
                    .typeOfAbsence(dto.reason())
                    .assignedTo(dto.assignedTo())
                    .build();
        } catch (EntityNotFoundException ex) {
            throw new NotFoundException(ex.getMessage());
        }
    }
}
