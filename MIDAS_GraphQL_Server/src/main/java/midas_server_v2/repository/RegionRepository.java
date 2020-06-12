package midas_server_v2.repository;

import midas_server_v2.model.tables.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public interface RegionRepository extends JpaRepository<Region, Integer> {

    Region findByIdReg(Integer id_reg);
}
