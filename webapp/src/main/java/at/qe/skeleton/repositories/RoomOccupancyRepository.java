package at.qe.skeleton.repositories;

import at.qe.skeleton.model.RoomOccupancy;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomOccupancyRepository extends CrudRepository<RoomOccupancy, String> {
}
