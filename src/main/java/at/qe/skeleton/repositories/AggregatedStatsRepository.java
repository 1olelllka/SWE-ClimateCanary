package at.qe.skeleton.repositories;

import at.qe.skeleton.model.AggregatedStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AggregatedStatsRepository extends JpaRepository<AggregatedStats, Integer> {
}
