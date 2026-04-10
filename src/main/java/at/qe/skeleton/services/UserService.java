package at.qe.skeleton.services;

import at.qe.skeleton.model.Userx;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {
    Page<Userx> getPageOfUsers(Pageable pageable);

    Userx getSpecificUser(UUID id);

    void deleteUser(UUID id);

    Userx updateUser(UUID id, Userx dto);

    Userx createNewUser(Userx userx);
}
