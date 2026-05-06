package at.qe.skeleton.repositories;

import at.qe.skeleton.model.UserClockStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserClockStatusRepository extends CrudRepository<UserClockStatus, String> {
}
