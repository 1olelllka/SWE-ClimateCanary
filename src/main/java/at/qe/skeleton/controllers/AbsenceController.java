package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.AbsenceCreateDTO;
import at.qe.skeleton.dtos.AbsenceDTO;
import at.qe.skeleton.dtos.AbsenceListDTO;
import at.qe.skeleton.dtos.AbsencePatchDTO;
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
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('DEPARTMENT_MANAGER')")
    public ResponseEntity<Page<AbsenceListDTO>> getAllAbsences(Authentication authentication,
                                                               Pageable pageable) {
        Set<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        if (authorities.contains("ROLE_DEPARTMENT_MANAGER")) {
            Userx user = authenticatedUserService.getAuthenticatedUser();
            Page<Absence> absences = absenceService.getAllAbsencesByDepartment(user, pageable);
            return new ResponseEntity<>(absences.map(absenceListMapper::mapTo), HttpStatus.OK);
        } else if (authorities.contains("ROLE_EMPLOYEE")) {
            Userx user = authenticatedUserService.getAuthenticatedUser();
            return new ResponseEntity<>(absenceService.getAllAbsencesById(user.getId(), pageable).map(absenceListMapper::mapTo), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
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
        Absence absence = absenceService.createNewAbsenceForUser(absenceCreateMapper.mapFrom(dto));
        return new ResponseEntity<>(absenceMapper.mapTo(absence), HttpStatus.CREATED);
    }

    @GetMapping("{absence_id}")
    @PreAuthorize("hasAuthority('CAN_MANAGE_ABSENCES')")
    public ResponseEntity<AbsenceDTO> getSpecificAbsence(@PathVariable(name="absence_id") UUID id) {
        Userx manager = authenticatedUserService.getAuthenticatedUser();
        Absence absence = absenceService.getAbsenceById(id, manager);
        return new ResponseEntity<>(absenceMapper.mapTo(absence), HttpStatus.OK);
    }

    @PatchMapping("{absence_id}")
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

    @DeleteMapping("{absence_id}")
    @PreAuthorize("hasAuthority('CAN_MANAGE_OWN_ABSENCE')")
    public ResponseEntity<Void> deleteSpecificAbsence(@PathVariable(name="absence_id") UUID id,
                                                      Authentication authentication) {
        Userx user = authenticatedUserService.getAuthenticatedUser();
        absenceService.deleteAbsenceById(id, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
