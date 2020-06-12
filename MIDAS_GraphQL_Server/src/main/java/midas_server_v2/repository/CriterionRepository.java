package midas_server_v2.repository;

import midas_server_v2.model.tables.Criterion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CriterionRepository extends JpaRepository<Criterion, Integer> {
}
