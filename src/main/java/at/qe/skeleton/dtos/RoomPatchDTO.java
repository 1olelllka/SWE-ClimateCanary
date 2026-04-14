package at.qe.skeleton.dtos;

import at.qe.skeleton.model.RoomType;
import at.qe.skeleton.model.Userx;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.Set;
import java.util.UUID;

public record RoomPatchDTO(
        UUID departmentID,
        RoomType roomType,
        boolean isActive,
        @Min(value = 1, message = "People count must not be less than one.")
        Integer defaultPeopleCount,
        Set<UUID> users,
        @Pattern(regexp = "^(?!\\s*$).+", message = "Name must not be blank.")
        String name
) {
}
