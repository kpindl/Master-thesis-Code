package midas_server_v2.repository;

import midas_server_v2.model.tables.Run;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface RunRepository extends JpaRepository<Run, Integer> {
    List<Run> findByIlluminarunname(String illuminarunname);
}
