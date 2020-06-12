package midas_server_v2.repository;

import midas_server_v2.model.tables.CNV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public interface CNVRepository extends JpaRepository<CNV, Integer> {

    @Query("SELECT c FROM CNV c WHERE c.idSam = ?1")
    List<CNV> findBySampleId(int id_sam);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE CNV c SET c.hgvs = ?2, c.iscn = ?3 WHERE c.idCnv = ?1")
    void updateNomenclature(int id_cnv, String hgvs, String iscn);
}
