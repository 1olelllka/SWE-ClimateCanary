package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.AbsenceCreateMapper;
import at.qe.skeleton.mappers.AbsenceListMapper;
import at.qe.skeleton.mappers.AbsenceMapper;
import at.qe.skeleton.model.Absence;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.AbsenceService;
import at.qe.skeleton.services.AuthenticatedUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/absences")
public class AbsenceController {

    private AbsenceService absenceService;
    private AbsenceListMapper absenceListMapper;
    private AbsenceCreateMapper absenceCreateMapper;
    private AbsenceMapper absenceMapper;
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    public AbsenceController(AbsenceService absenceService,
                             AbsenceListMapper absenceListMapper,
                             AbsenceCreateMapper absenceCreateMapper,
                             AbsenceMapper absenceMapper,
                             AuthenticatedUserService authenticatedUserService) {
        this.absenceService = absenceService;
        this.absenceListMapper = absenceListMapper;
        this.absenceCreateMapper = absenceCreateMapper;
        this.absenceMapper = absenceMapper;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("")
    @PreAuthorize("hasAuthority('CAN_MANAGE_OWN_ABSENCE') or hasAuthority('CAN_VIEW_ABSENCE_VIEW')")
    public ResponseEntity<Page<AbsenceListDTO>> getAllAbsences(Authentication authentication,
                                                               Pageable pageable) {
        Set<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        Userx user = authenticatedUserService.getAuthenticatedUser();
        if (authorities.contains("CAN_VIEW_ABSENCE_VIEW")) {
            Page<Absence> absences = absenceService.getAllAbsencesByDepartment(user, pageable);
            return new ResponseEntity<>(absences.map(absenceListMapper::mapTo), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(absenceService.getAllAbsencesById(user.getId(), pageable).map(absenceListMapper::mapTo), HttpStatus.OK);
        }
    }

    @GetMapping("/managers")
    @PreAuthorize("hasAuthority('CAN_MANAGE_OWN_ABSENCE')")
    public ResponseEntity<List<UserxListDTO>> getAvailableManagersForAbsence() {
        Userx user = authenticatedUserService.getAuthenticatedUser();
        List<Userx> managers = absenceService.getAllAvailableManagersForUser(user);
        return new ResponseEntity<>(managers.stream().map(man ->
            new UserxListDTO(man.getId(), man.getCreateDate(), man.getUsername(), man.getFirstName(), man.getLastName())
        ).toList(), HttpStatus.OK);
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('CAN_MANAGE_OWN_ABSENCE')")
    public ResponseEntity<AbsenceDTO> createNewAbsence(@RequestBody @Valid AbsenceCreateDTO dto,
                                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        if (dto.startDate().isAfter(dto.endDate())) {
            throw new ValidationException("Start date must not be after end date.");
        }
        if (dto.startDate().toLocalDate().isBefore(LocalDate.now())) {
            throw new ValidationException("Starting date must be in present or future.");
        }

        if (dto.endDate().toLocalDate().isBefore(LocalDate.now())) {
            throw new ValidationException("Ending date must be in present or future.");
        }
        Absence absence = absenceService.createNewAbsenceForUser(absenceCreateMapper.mapFrom(dto));
        return new ResponseEntity<>(absenceMapper.mapTo(absence), HttpStatus.CREATED);
    }

    @GetMapping("/clock-status")
    public ResponseEntity<ClockStatusDTO> getClockStatus() {
        Userx user = authenticatedUserService.getAuthenticatedUser();
        boolean clockedIn = absenceService.isClockedIn(user);
        return new ResponseEntity<>(new ClockStatusDTO(clockedIn), HttpStatus.OK);
    }

    @GetMapping("/{absence_id}")
    @PreAuthorize("hasAuthority('CAN_MANAGE_ABSENCES')")
    public ResponseEntity<AbsenceDTO> getSpecificAbsence(@PathVariable(name="absence_id") UUID id) {
        Userx manager = authenticatedUserService.getAuthenticatedUser();
        Absence absence = absenceService.getAbsenceById(id, manager);
        return new ResponseEntity<>(absenceMapper.mapTo(absence), HttpStatus.OK);
    }

    @PatchMapping("/{absence_id}")
    @PreAuthorize("hasAuthority('CAN_MANAGE_ABSENCES')")
    public ResponseEntity<AbsenceDTO> updateStatusOfAbsence(@PathVariable(name="absence_id") UUID id,
                                                            @RequestBody @Valid AbsencePatchDTO dto,
                                                            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Absence patched = absenceService.updateAbsenceStatus(id, dto.status());
        return new ResponseEntity<>(absenceMapper.mapTo(patched), HttpStatus.OK);
    }

    @PatchMapping("/{absence_id}/cancel")
    @PreAuthorize("hasAuthority('CAN_MANAGE_OWN_ABSENCE')")
    public ResponseEntity<AbsenceDTO> cancelAbsence(@PathVariable(name="absence_id") UUID id) {
        Userx user = authenticatedUserService.getAuthenticatedUser();
        Absence cancelled = absenceService.cancelAbsence(id, user);
        return new ResponseEntity<>(absenceMapper.mapTo(cancelled), HttpStatus.OK);
    }

    @PostMapping("/clock-in")
    public ResponseEntity<Void> clockInForSpecificRoom() {
        Userx user = authenticatedUserService.getAuthenticatedUser();
        absenceService.clockIn(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/clock-out")
    public ResponseEntity<Void> clockOutForSpecificRoom() {
        Userx user = authenticatedUserService.getAuthenticatedUser();
        absenceService.clockOut(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
