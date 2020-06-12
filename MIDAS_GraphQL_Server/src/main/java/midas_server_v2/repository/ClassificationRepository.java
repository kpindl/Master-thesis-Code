package midas_server_v2.repository;

import midas_server_v2.model.tables.Classification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public interface ClassificationRepository extends JpaRepository<Classification, Integer> {
}
