package midas_server_v2.repository;

import midas_server_v2.model.tables.TargetCaller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface TargetCallerRepository extends JpaRepository<TargetCaller, Integer> {

    List<TargetCaller> findByIdSat(Integer id_sat);
}
