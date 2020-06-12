package midas_server_v2.repository;

import midas_server_v2.model.tables.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface TargetRepository extends JpaRepository<Target, Integer> {

    List<Target> findByIdCnv(Integer id_cnv);

    @Query(nativeQuery = true,
            value = "SELECT COUNT(*) AS count " +
            "FROM ngs.sam_target st " +
            "JOIN (SELECT    id_sat, COUNT(*) AS evidence " +
            "       FROM     ngs.sam_tar_cal " +
            "       GROUP BY id_sat) AS e  " +
            "ON    st.id_sat = e.id_sat " +
            "JOIN  ngs.sam_cnv sc " +
            "ON    sc.id_cnv = st.id_cnv " +
            "JOIN  ngs.sample s " +
            "ON    s.id_sam = sc.id_sam " +
            "WHERE st.id_reg = ?1 " +
            "AND   e.evidence = ?2 " +
            "AND   s.id_enrvers = ?3 " +
            "GROUP BY st.id_reg, e.evidence, s.id_enrvers ")
    Integer getNumberSamplesIH(Integer id_reg, Integer evidence, Integer id_enrvers);
}
