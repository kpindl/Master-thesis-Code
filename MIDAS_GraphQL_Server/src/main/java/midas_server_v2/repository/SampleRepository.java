package midas_server_v2.repository;

import midas_server_v2.model.tables.Sample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface SampleRepository extends JpaRepository<Sample, Integer> {

    List<Sample> findByIdEnrvers(Integer id_enrvers);
}
