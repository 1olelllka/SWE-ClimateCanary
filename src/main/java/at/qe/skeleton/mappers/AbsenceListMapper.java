package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.AbsenceListDTO;
import at.qe.skeleton.model.Absence;
import at.qe.skeleton.model.Userx;
import org.springframework.stereotype.Component;

@Component
public class AbsenceListMapper implements DTOMapper<Absence, AbsenceListDTO> {
    @Override
    public AbsenceListDTO mapTo(Absence entity) {
        if (entity.getUser() == null) {
            return new AbsenceListDTO(entity.getId(),
                    null,
                    null,
                    null,
                    null,
                    entity.getStartDate(),
                    entity.getEndDate(),
                    entity.getTypeOfAbsence(),
                    entity.getStatus(),
                    entity.getCreatedAt());
        }
        return new AbsenceListDTO(entity.getId(),
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
                entity.getCreatedAt());
    }

    @Override
    public Absence mapFrom(AbsenceListDTO dto) {
        return Absence
                .builder()
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
