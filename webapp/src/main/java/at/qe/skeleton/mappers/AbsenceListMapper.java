package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.AbsenceListDTO;
import at.qe.skeleton.model.Absence;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.UserxRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AbsenceListMapper implements DTOMapper<Absence, AbsenceListDTO> {

    private final UserxRepository userxRepository;

    public AbsenceListMapper(UserxRepository userxRepository) {
        this.userxRepository = userxRepository;
    }

    @Override
    public AbsenceListDTO mapTo(Absence entity) {
        String managerFirstName = null;
        String managerLastName = null;
        if (entity.getAssignedTo() != null) {
            Optional<Userx> manager = userxRepository.findById(entity.getAssignedTo());
            if (manager.isPresent()) {
                managerFirstName = manager.get().getFirstName();
                managerLastName  = manager.get().getLastName();
            }
        }

        if (entity.getUser() == null) {
            return new AbsenceListDTO(
                    entity.getId(),
                    null, null, null, null,
                    entity.getStartDate(),
                    entity.getEndDate(),
                    entity.getTypeOfAbsence(),
                    entity.getStatus(),
                    entity.getCreatedAt(),
                    entity.getComment(),
                    managerFirstName,
                    managerLastName);
        }

        return new AbsenceListDTO(
                entity.getId(),
                entity.getUser().getId(),
                entity.getUser().getFirstName(),
                entity.getUser().getLastName(),
                entity.getUser().getMyRoom() != null
                        ? entity.getUser().getMyRoom().getRoomNumber()
                        : null,
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getTypeOfAbsence(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getComment(),
                managerFirstName,
                managerLastName);
    }

    @Override
    public Absence mapFrom(AbsenceListDTO dto) {
        return Absence.builder()
                .id(dto.id())
                .user(Userx.builder().id(dto.userId()).build())
                .status(dto.status())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .createdAt(dto.createdAt())
                .typeOfAbsence(dto.typeOfAbsence())
                .build();
    }
}
