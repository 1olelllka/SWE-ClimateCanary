package at.qe.skeleton.repositories;

import at.qe.skeleton.model.FormulaWeights;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormulaWeightsRepository extends JpaRepository<FormulaWeights, Integer> {
}
