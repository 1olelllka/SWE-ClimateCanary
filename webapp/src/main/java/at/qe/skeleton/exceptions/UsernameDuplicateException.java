package at.qe.skeleton.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown for duplicate username.
 * Uses {@link ResponseStatus} to return status {@code 409 (Conflict)} 
 * when thrown in a Spring REST Controller.
 * 
 * This class is part of the skeleton project provided for students of the
* course "Software Engineering" offered by Innsbruck University.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class UsernameDuplicateException extends RuntimeException {
    public UsernameDuplicateException(String message) {
        super(message);
    }
}
