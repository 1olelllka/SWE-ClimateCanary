package at.qe.skeleton.repositories;


import at.qe.skeleton.model.RaspberryPi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RaspberryPiRepository extends JpaRepository<RaspberryPi, UUID> {
    boolean existsByIpAndPort(String ip, Integer port);
    boolean existsByName(String name);
}
