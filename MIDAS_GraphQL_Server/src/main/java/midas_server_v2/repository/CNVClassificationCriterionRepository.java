package midas_server_v2.repository;

import midas_server_v2.model.tables.CNVClassificationCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CNVClassificationCriterionRepository extends JpaRepository<CNVClassificationCriterion, Integer> {
}
