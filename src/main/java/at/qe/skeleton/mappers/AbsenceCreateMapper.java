package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.AbsenceCreateDTO;
import at.qe.skeleton.model.Absence;
import at.qe.skeleton.repositories.UserxRepository;
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
        return Absence
                .builder()
                .user(userxRepository.getReferenceById(dto.userId()))
                .comment(dto.comment())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .typeOfAbsense(dto.reason())
                .assignedTo(dto.assignedTo())
                .build();
    }
}
