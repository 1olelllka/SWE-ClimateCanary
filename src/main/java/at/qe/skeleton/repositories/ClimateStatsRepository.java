package at.qe.skeleton.repositories;

import at.qe.skeleton.model.ClimateStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.UUID;

@Repository
public interface ClimateStatsRepository extends JpaRepository<ClimateStats, UUID> {

}